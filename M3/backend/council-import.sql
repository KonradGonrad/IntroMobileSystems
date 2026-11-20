-- Apply once after schema.sql, including on existing M3 installations.
begin;

alter table public.profiles add column is_council boolean not null default false;
alter table public.listings add column fair_ref text
  check (fair_ref ~ '^[A-Z0-9][A-Z0-9_-]{0,39}$');
alter table public.listings add unique (owner_id, fair_ref);

create function public.is_student_council() returns boolean
language sql stable set search_path = '' as $$
  select exists (select 1 from public.profiles where id = auth.uid() and is_council);
$$;

create or replace function public.feed_snapshot() returns jsonb
language sql stable security definer set search_path = '' as $$
 select coalesce(jsonb_agg(jsonb_build_object(
  'id',l.id,'title',l.title,'description',l.description,'category',l.category,'condition',l.condition,
  'owner_name',p.display_name,'is_council',p.is_council,'photo_path',l.photo_path,'is_mine',l.owner_id=auth.uid(),
  'is_liked',exists(select 1 from public.likes k where k.listing_id=l.id and k.user_id=auth.uid()),
  'reserved',l.reserved_by is not null,'reserved_by_me',coalesce(l.reserved_by=auth.uid(),false),
  'created_at',(extract(epoch from l.created_at)*1000)::bigint
 ) order by l.created_at desc),'[]'::jsonb)
 from public.listings l join public.profiles p on p.id=l.owner_id where not l.deleted and auth.uid() is not null;
$$;

-- With no expected preview this RPC is read-only. A confirmation must match
-- the current plan, including the previous field values the council reviewed.
create function public.council_import(entries jsonb, expected jsonb default null, overwrite_existing boolean default false)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
  council_id uuid := auth.uid();
  plan jsonb;
begin
  perform 1 from public.profiles where id = council_id and is_council for share;
  if not found then raise exception 'Student council account required'; end if;
  if jsonb_typeof(entries) is distinct from 'array' then raise exception 'Expected a list of CSV rows'; end if;
  if jsonb_array_length(entries) not between 1 and 500 then raise exception 'Choose 1 to 500 listings per import'; end if;
  if exists (
    select 1 from jsonb_to_recordset(entries) as r(ref text, title text, description text, category text, condition text)
    where (r.ref ~ '^[A-Z0-9][A-Z0-9_-]{0,39}$'
      and length(btrim(r.title)) between 3 and 60 and length(btrim(r.description)) between 10 and 2000
      and r.category in ('CLOTHES','ELECTRONICS','BOOKS','KITCHEN','FURNITURE','OTHER')
      and r.condition in ('NEW','LIKE_NEW','VERY_GOOD','GOOD','USED')) is not true
  ) then raise exception 'Invalid listing in CSV'; end if;
  if exists (select 1 from jsonb_array_elements(entries) e group by e->>'ref' having count(*) > 1)
    then raise exception 'Duplicate fair reference'; end if;

  if expected is not null then
    -- Same owner lock as apply_action. Row locks also serialize reservations by other users.
    perform pg_advisory_xact_lock(hashtextextended(council_id::text, 0));
    perform 1 from public.listings where owner_id = council_id
      and fair_ref in (select e->>'ref' from jsonb_array_elements(entries) e) order by id for update;
  end if;

  with matches as (
    select n, e->>'ref' as ref, l.id, l.title, l.description, l.category, l.condition,
      case when l.id is null then 'new' when l.deleted then 'deleted' when l.reserved_by is not null then 'reserved'
        when (l.title, l.description, l.category, l.condition) =
          (btrim(e->>'title'), btrim(e->>'description'), e->>'category', e->>'condition') then 'unchanged'
        else 'changed' end as status
    from jsonb_array_elements(entries) with ordinality as rows(e,n)
    left join public.listings l on l.owner_id = council_id and l.fair_ref = e->>'ref'
  )
  select jsonb_agg(jsonb_build_object('ref', ref, 'status', status, 'id', id, 'previous',
    case when status = 'changed' then jsonb_build_object(
      'title', title, 'description', description, 'category', category, 'condition', condition) end) order by n)
  into plan from matches;

  if expected is null then return plan; end if;
  if expected <> plan then raise exception 'Listings changed. Review a fresh preview before importing.'; end if;

  insert into public.listings(id, owner_id, fair_ref, title, description, category, condition)
    select gen_random_uuid(), council_id, r.ref, btrim(r.title), btrim(r.description), r.category, r.condition
    from jsonb_to_recordset(entries) as r(ref text, title text, description text, category text, condition text)
    join jsonb_to_recordset(plan) as p(ref text, status text) using (ref)
    where p.status = 'new';

  if overwrite_existing then
    update public.listings l set title = btrim(r.title), description = btrim(r.description),
      category = r.category, condition = r.condition
    from jsonb_to_recordset(entries) as r(ref text, title text, description text, category text, condition text)
    join jsonb_to_recordset(plan) as p(ref text, status text) using (ref)
    where l.owner_id = council_id and l.fair_ref = r.ref and p.status = 'changed';
  end if;
  return plan;
end $$;

revoke execute on function public.is_student_council(), public.council_import(jsonb,jsonb,boolean) from public, anon;
grant execute on function public.is_student_council(), public.council_import(jsonb,jsonb,boolean) to authenticated;
notify pgrst, 'reload schema';
commit;
