-- Run once in a NEW Supabase project's SQL Editor. No service key belongs in the APK.
begin;
create table public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  display_name text not null check (length(btrim(display_name)) between 2 and 60)
);
create function public.create_profile() returns trigger language plpgsql security definer set search_path = '' as $$
begin
  if lower(new.email) !~ '^[^@[:space:]]+@edu\.p\.lodz\.pl$' then
    raise exception 'Only @edu.p.lodz.pl addresses are allowed';
  end if;
  insert into public.profiles values(new.id, btrim(new.raw_user_meta_data->>'display_name'));
  return new;
end $$;
create trigger dormswap_signup after insert on auth.users for each row execute function public.create_profile();
-- Also reject changing a registered account to a different email domain via the Auth API.
create function public.check_email_domain() returns trigger language plpgsql set search_path = '' as $$
begin
  if lower(new.email) !~ '^[^@[:space:]]+@edu\.p\.lodz\.pl$' then raise exception 'Invalid university domain'; end if;
  return new;
end $$;
create trigger dormswap_email before update of email on auth.users for each row execute function public.check_email_domain();

create table public.listings (
 id uuid primary key, owner_id uuid not null references public.profiles(id),
 title text not null check(length(btrim(title)) between 3 and 60),
 description text not null check(length(btrim(description)) between 10 and 2000),
 category text not null check(category in ('CLOTHES','ELECTRONICS','BOOKS','KITCHEN','FURNITURE','OTHER')),
 condition text not null check(condition in ('NEW','LIKE_NEW','VERY_GOOD','GOOD','USED')),
 photo_path text check(photo_path is null or photo_path ~ '^[a-fA-F0-9-]{36}/[a-f0-9]{64}\.jpg$'), reserved_by uuid references public.profiles(id), deleted boolean not null default false,
 created_at timestamptz not null default now(), check(reserved_by is distinct from owner_id)
);
create table public.likes (
 user_id uuid references public.profiles(id) on delete cascade,
 listing_id uuid references public.listings(id) on delete cascade,
 primary key(user_id, listing_id)
);
create table public.conversations (
 id uuid primary key default gen_random_uuid(), listing_id uuid not null references public.listings(id),
 buyer_id uuid not null references public.profiles(id), owner_id uuid not null references public.profiles(id),
 unique(listing_id,buyer_id), check(buyer_id <> owner_id)
);
create table public.messages (
 id uuid primary key, conversation_id uuid not null references public.conversations(id),
 sender_id uuid not null references public.profiles(id), body text not null check(length(btrim(body)) between 1 and 2000),
 created_at timestamptz not null default now()
);
create index messages_conversation_time on public.messages(conversation_id,created_at);
create table public.processed_actions (
 user_id uuid references public.profiles(id) on delete cascade, id uuid, primary key(user_id,id)
);

alter table public.profiles enable row level security;
alter table public.listings enable row level security;
alter table public.likes enable row level security;
alter table public.conversations enable row level security;
alter table public.messages enable row level security;
alter table public.processed_actions enable row level security;
-- Writes only through the checked RPC below. No direct client DML grants.
revoke all on public.profiles, public.listings, public.likes, public.conversations, public.messages, public.processed_actions from anon, authenticated;
grant select on public.profiles, public.listings, public.likes, public.conversations, public.messages to authenticated;
create policy profiles_read on public.profiles for select to authenticated using (true);
create policy listings_read on public.listings for select to authenticated using (not deleted or owner_id=auth.uid());
create policy likes_read on public.likes for select to authenticated using(user_id=auth.uid());
create policy conversations_read on public.conversations for select to authenticated using(auth.uid() in (buyer_id,owner_id));
create policy messages_read on public.messages for select to authenticated using(exists(
 select 1 from public.conversations c where c.id=conversation_id and auth.uid() in(c.buyer_id,c.owner_id)));

create function public.apply_action(action_id uuid, kind text, target_id uuid, payload jsonb)
returns void language plpgsql security definer set search_path = '' as $$
declare u uuid := auth.uid(); l public.listings; c public.conversations;
begin
 if u is null then raise exception 'Sign in required'; end if;
 -- Serialise retries from the same user. Operation IDs make retries idempotent.
 perform pg_advisory_xact_lock(hashtextextended(u::text,0));
 if exists(select 1 from public.processed_actions where user_id=u and id=action_id) then return; end if;
 if kind='message' then
   select * into c from public.conversations where id=(payload->>'conversation_id')::uuid;
   if c.id is null or u not in(c.buyer_id,c.owner_id) then raise exception 'Conversation access denied'; end if;
   insert into public.messages(id,conversation_id,sender_id,body) values(target_id,c.id,u,btrim(payload->>'body'));
 else
   select * into l from public.listings where id=target_id for update;
   if kind='save' then
     if l.id is not null and (l.owner_id<>u or l.deleted or l.reserved_by is not null) then
       raise exception 'Listing cannot be edited while reserved or unavailable';
     end if;
     if payload->>'photo_path' is not null and (payload->>'photo_path') not like u::text || '/%' then
       raise exception 'Photo access denied';
     end if;
     insert into public.listings(id,owner_id,title,description,category,condition,photo_path)
     values(target_id,u,btrim(payload->>'title'),btrim(payload->>'description'),payload->>'category',payload->>'condition',payload->>'photo_path')
     on conflict(id) do update set title=excluded.title,description=excluded.description,category=excluded.category,
       condition=excluded.condition,photo_path=excluded.photo_path;
   elsif l.id is null or l.deleted then raise exception 'Listing is no longer available';
   elsif kind='delete' then
     if l.owner_id<>u then raise exception 'Only the owner can delete this listing'; end if;
     update public.listings set deleted=true,reserved_by=null where id=target_id;
   elsif kind='reserve' then
     if l.owner_id=u then raise exception 'Cannot reserve your own listing'; end if;
     if l.reserved_by is not null and l.reserved_by<>u then raise exception 'Someone else reserved this item first'; end if;
     update public.listings set reserved_by=u where id=target_id;
   elsif kind='cancel' then
     if l.reserved_by is not null and l.reserved_by<>u then raise exception 'This reservation belongs to another student'; end if;
     update public.listings set reserved_by=null where id=target_id;
   elsif kind='like' then
     if (payload->>'liked')::boolean then
       insert into public.likes values(u,target_id) on conflict do nothing;
     else delete from public.likes where user_id=u and listing_id=target_id; end if;
   else raise exception 'Unknown action'; end if;
 end if;
 insert into public.processed_actions values(u,action_id);
end $$;

create function public.start_conversation(listing_id uuid) returns uuid
language plpgsql security definer set search_path = '' as $$
declare l public.listings; result uuid; u uuid := auth.uid();
begin
 if u is null then raise exception 'Sign in required'; end if;
 select * into l from public.listings where id=listing_id;
 if l.id is null or l.deleted or l.owner_id=u then raise exception 'Cannot start this conversation'; end if;
 insert into public.conversations(listing_id,buyer_id,owner_id) values(l.id,u,l.owner_id)
 on conflict on constraint conversations_listing_id_buyer_id_key do update set listing_id=excluded.listing_id returning id into result;
 return result;
end $$;

create function public.feed_snapshot() returns jsonb language sql stable security definer set search_path = '' as $$
 select coalesce(jsonb_agg(jsonb_build_object(
  'id',l.id,'title',l.title,'description',l.description,'category',l.category,'condition',l.condition,
  'owner_name',p.display_name,'photo_path',l.photo_path,'is_mine',l.owner_id=auth.uid(),
  'is_liked',exists(select 1 from public.likes k where k.listing_id=l.id and k.user_id=auth.uid()),
  'reserved',l.reserved_by is not null,'reserved_by_me',coalesce(l.reserved_by=auth.uid(),false),
  'created_at',(extract(epoch from l.created_at)*1000)::bigint
 ) order by l.created_at desc),'[]'::jsonb)
 from public.listings l join public.profiles p on p.id=l.owner_id where not l.deleted and auth.uid() is not null;
$$;
create function public.chat_snapshot() returns jsonb language sql stable security definer set search_path = '' as $$
 select jsonb_build_object(
 'conversations',coalesce((select jsonb_agg(jsonb_build_object('id',c.id,'listing_id',l.id,'title',l.title,
 'other_name',p.display_name,'unavailable',l.deleted)) from public.conversations c
 join public.listings l on l.id=c.listing_id join public.profiles p on p.id=case when c.owner_id=auth.uid() then c.buyer_id else c.owner_id end
 where auth.uid() in(c.buyer_id,c.owner_id)),'[]'::jsonb),
 'messages',coalesce((select jsonb_agg(jsonb_build_object('id',m.id,'conversation_id',m.conversation_id,
 'sender_id',m.sender_id,'body',m.body,'created_at',(extract(epoch from m.created_at)*1000)::bigint) order by m.created_at)
 from public.messages m join public.conversations c on c.id=m.conversation_id where auth.uid() in(c.buyer_id,c.owner_id)),'[]'::jsonb));
$$;

revoke execute on function public.create_profile(), public.check_email_domain(), public.apply_action(uuid,text,uuid,jsonb),
 public.start_conversation(uuid), public.feed_snapshot(), public.chat_snapshot() from public, anon;
grant execute on function public.apply_action(uuid,text,uuid,jsonb), public.start_conversation(uuid), public.feed_snapshot(), public.chat_snapshot() to authenticated;

insert into storage.buckets(id,name,public,file_size_limit,allowed_mime_types)
 values('listing-photos','listing-photos',false,5242880,array['image/jpeg']);
create policy photo_insert on storage.objects for insert to authenticated
 with check(bucket_id='listing-photos' and (storage.foldername(name))[1]=auth.uid()::text);
create policy photo_read on storage.objects for select to authenticated using(bucket_id='listing-photos' and
 ((storage.foldername(name))[1]=auth.uid()::text or exists(select 1 from public.listings l where l.photo_path=name and not l.deleted)));
create policy photo_delete on storage.objects for delete to authenticated
 using(bucket_id='listing-photos' and (storage.foldername(name))[1]=auth.uid()::text);
commit;
