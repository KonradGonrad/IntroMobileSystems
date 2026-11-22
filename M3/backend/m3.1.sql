-- Apply after council-import.sql in a single transaction.
begin;
alter table public.listings add column reserved_until timestamptz;
update public.listings set reserved_until=now()+interval '24 hours' where reserved_by is not null;
create index listing_expiry on public.listings(reserved_until) where reserved_until is not null;
create table public.reservation_blocks (
 listing_id uuid references public.listings, user_id uuid references public.profiles,
 until_at timestamptz not null, primary key(listing_id,user_id)
);
create table public.activity_events (
 id uuid primary key default gen_random_uuid(), recipient uuid not null references public.profiles on delete cascade,
 kind text not null check(kind in ('reserve','message','like')), summary text not null,
 created_at timestamptz not null default now()
);
create index recipient_activity on public.activity_events(recipient,created_at desc);
create table public.push_devices(token text primary key, user_id uuid not null references public.profiles on delete cascade);
alter table public.activity_events enable row level security;
alter table public.reservation_blocks enable row level security;
alter table public.push_devices enable row level security;
revoke all on public.activity_events,public.reservation_blocks,public.push_devices from public,anon,authenticated;

create function public.register_device(device_token text) returns void
language plpgsql security definer set search_path='' as $$
begin
 if auth.uid() is null or length(device_token) not between 20 and 4096 then raise exception 'Invalid device'; end if;
 insert into public.push_devices values(device_token,auth.uid())
 on conflict(token) do update set user_id=excluded.user_id;
end $$;

create function public.activity_snapshot() returns jsonb language sql stable security definer set search_path='' as $$
 select coalesce(jsonb_agg(to_jsonb(e) order by e.created_at desc,e.id),'[]') from
 (select id,kind,summary,created_at from public.activity_events where recipient=auth.uid() order by created_at desc,id limit 50) e;
$$;

create function public.reservation_change() returns trigger language plpgsql security definer set search_path='' as $$
begin
 if new.reserved_by is not distinct from old.reserved_by then return new; end if;
 if old.reserved_by is not null then
   insert into public.reservation_blocks values(old.id,old.reserved_by,least(now(),old.reserved_until)+interval '24 hours')
    on conflict(listing_id,user_id) do update set until_at=excluded.until_at;
 end if;
 if new.reserved_by is null then
   new.reserved_until=null;
 else
   -- apply_action serializes operations per user and locks the listing.
   if exists(select 1 from public.reservation_blocks where listing_id=new.id and user_id=new.reserved_by and until_at>now())
    then raise exception 'Please wait 24 hours before reserving this listing again'; end if;
   if (select count(*) from public.listings where reserved_by=new.reserved_by and reserved_until>now() and not deleted)>=3
    then raise exception 'You can have up to 3 active reservations'; end if;
   new.reserved_until=now()+interval '24 hours';
   insert into public.activity_events(recipient,kind,summary)
    select new.owner_id,'reserve',display_name || ' reserved ' || new.title from public.profiles where id=new.reserved_by;
 end if;
 return new;
end $$;
create trigger reservation_change before update of reserved_by on public.listings
 for each row execute function public.reservation_change();

create function public.record_activity() returns trigger language plpgsql security definer set search_path='' as $$
begin
 if tg_table_name='likes' then
  insert into public.activity_events(recipient,kind,summary)
   select l.owner_id,'like',p.display_name || ' liked ' || l.title from public.listings l
   join public.profiles p on p.id=new.user_id where l.id=new.listing_id and l.owner_id<>new.user_id;
 else
  insert into public.activity_events(recipient,kind,summary)
   select case when new.sender_id=c.owner_id then c.buyer_id else c.owner_id end,'message',
   p.display_name || ' sent a message about ' || l.title from public.conversations c
   join public.listings l on l.id=c.listing_id join public.profiles p on p.id=new.sender_id where c.id=new.conversation_id;
 end if;
 return new;
end $$;
create trigger liked_activity after insert on public.likes for each row execute function public.record_activity();
create trigger message_activity after insert on public.messages for each row execute function public.record_activity();

alter function public.apply_action(uuid,text,uuid,jsonb) rename to apply_action_m3;
revoke all on function public.apply_action_m3(uuid,text,uuid,jsonb) from public,anon,authenticated;
create function public.apply_action(action_id uuid,kind text,target_id uuid,payload jsonb)
returns void language plpgsql security definer set search_path='' as $$
begin
 if auth.uid() is null then raise exception 'Sign in required'; end if;
 perform pg_advisory_xact_lock(hashtextextended(auth.uid()::text,0));
 if kind <> 'message' then
   update public.listings set reserved_by=null where id=target_id and reserved_until<=now();
 end if;
 perform public.apply_action_m3(action_id,kind,target_id,payload);
end $$;

-- Expiry is evaluated using server time on every feed read and before writes.
create or replace function public.feed_snapshot() returns jsonb language plpgsql security definer set search_path='' as $$
begin
 if auth.uid() is null then return '[]'; end if;
 update public.listings set reserved_by=null where reserved_until<=now();
 return (select coalesce(jsonb_agg(jsonb_build_object(
 'id',l.id,'title',l.title,'description',l.description,'category',l.category,'condition',l.condition,
 'owner_name',p.display_name,'is_council',p.is_council,'photo_path',l.photo_path,'is_mine',l.owner_id=auth.uid(),
 'is_liked',exists(select 1 from public.likes k where k.listing_id=l.id and k.user_id=auth.uid()),
 'reserved',l.reserved_by is not null,'reserved_by_me',coalesce(l.reserved_by=auth.uid(),false),
 'reserver',r.display_name,'expires',(extract(epoch from l.reserved_until)*1000)::bigint,
 'blocked_until',(extract(epoch from b.until_at)*1000)::bigint,
 'created_at',(extract(epoch from l.created_at)*1000)::bigint) order by l.created_at desc),'[]')
 from public.listings l join public.profiles p on p.id=l.owner_id left join public.profiles r on r.id=l.reserved_by
 left join public.reservation_blocks b on b.listing_id=l.id and b.user_id=auth.uid() and b.until_at>now()
 where not l.deleted);
end $$;
revoke all on function public.reservation_change(),public.record_activity(),public.register_device(text),public.activity_snapshot(),public.apply_action(uuid,text,uuid,jsonb) from public,anon,authenticated;
grant execute on function public.register_device(text),public.activity_snapshot(),public.apply_action(uuid,text,uuid,jsonb) to authenticated;
-- With Supabase Cron enabled, also clear idle listings without a client read.
do $$ begin
 if exists(select 1 from pg_extension where extname='pg_cron') then
  perform cron.schedule('dormswap-release-expired', '* * * * *',
   'update public.listings set reserved_by=null where reserved_until<=now()');
 end if;
end $$;

-- Hosted Supabase's service role delivers webhooks. Local fixtures omit this role.
do $$ begin
 if exists(select 1 from pg_roles where rolname='service_role') then
  grant select on public.activity_events to service_role;
  grant select,delete on public.push_devices to service_role;
 end if;
end $$;
notify pgrst,'reload schema';
commit;
