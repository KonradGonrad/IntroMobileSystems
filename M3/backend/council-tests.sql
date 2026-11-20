-- Run with psql -v ON_ERROR_STOP=1 in the disposable local test database.
begin;
insert into auth.users(id,email,raw_user_meta_data) values
 ('a0000000-0000-0000-0000-000000000001','first.fair@edu.p.lodz.pl','{"display_name":"First council"}'),
 ('a0000000-0000-0000-0000-000000000002','second.fair@edu.p.lodz.pl','{"display_name":"Second council"}'),
 ('a0000000-0000-0000-0000-000000000003','visitor.fair@edu.p.lodz.pl','{"display_name":"Visitor","is_council":true}');
update public.profiles set is_council = true where display_name in ('First council', 'Second council')
  and id in ('a0000000-0000-0000-0000-000000000001','a0000000-0000-0000-0000-000000000002');
set local role authenticated;
do $$
declare
  council text := 'a0000000-0000-0000-0000-000000000001';
  second_council text := 'a0000000-0000-0000-0000-000000000002';
  visitor text := 'a0000000-0000-0000-0000-000000000003';
  rows jsonb := '[{"ref":"FAIR-1","title":"Desk lamp","description":"Working desk lamp","category":"OTHER","condition":"GOOD"}]';
  edited jsonb := jsonb_set(rows, '{0,title}', '"Reading lamp"');
  mixed jsonb := edited || jsonb_set(rows, '{0,ref}', '"FAIR-2"');
  preview jsonb;
  listing_id uuid;
  manual_id uuid := gen_random_uuid();
begin
  perform set_config('request.jwt.claim.sub', visitor, true);
  assert not public.is_student_council(), 'Registration metadata must not grant a council role';
  begin
    update public.profiles set is_council = true where id = visitor::uuid;
    raise exception 'Student could self-promote';
  exception when insufficient_privilege then null; end;
  begin
    perform public.council_import(rows, '[]', true);
    raise exception 'Student could import';
  exception when raise_exception then if sqlerrm <> 'Student council account required' then raise; end if; end;

  perform set_config('request.jwt.claim.sub', council, true);
  assert public.is_student_council();
  preview := public.council_import(rows);
  assert preview->0->>'status' = 'new' and (select count(*) from public.listings where owner_id=council::uuid) = 0;
  perform public.council_import(rows, preview);
  select id into strict listing_id from public.listings where owner_id = council::uuid and fair_ref = 'FAIR-1';
  assert public.council_import(rows)->0->>'status' = 'unchanged';
  assert exists(select 1 from jsonb_array_elements(public.feed_snapshot()) j where j->>'id' = listing_id::text and (j->>'is_council')::boolean);
  preview := public.council_import(edited);
  assert preview->0->>'status' = 'changed' and preview->0->'previous'->>'title' = 'Desk lamp';
  perform public.council_import(edited, preview);
  assert (select title = 'Desk lamp' from public.listings where id = listing_id);
  perform public.council_import(edited, preview, true);
  assert (select title = 'Reading lamp' from public.listings where id = listing_id);
  -- Same title, no fair reference: an ordinary listing must never be captured by the import.
  perform public.apply_action(gen_random_uuid(), 'save', manual_id, rows->0);
  preview := public.council_import(rows);
  perform public.apply_action(gen_random_uuid(), 'save', listing_id,
    jsonb_set(edited->0, '{description}', '"Condition checked by another organizer."'));
  begin
    perform public.council_import(rows, preview, true);
    raise exception 'Changed previous field values were ignored';
  exception when raise_exception then if sqlerrm not like 'Listings changed.%' then raise; end if; end;
  perform public.council_import(edited, public.council_import(edited), true);
  assert (select fair_ref is null and title='Desk lamp' from public.listings where id=manual_id);

  perform set_config('request.jwt.claim.sub', second_council, true);
  assert public.council_import(rows)->0->>'status' = 'new';
  perform public.council_import(rows, public.council_import(rows), true);
  assert (select count(*) from public.listings where fair_ref='FAIR-1') = 2;
  assert (select title = 'Reading lamp' from public.listings where id=listing_id);

  perform set_config('request.jwt.claim.sub', council, true);
  preview := public.council_import(mixed);
  perform set_config('request.jwt.claim.sub', visitor, true);
  perform public.apply_action(gen_random_uuid(), 'reserve', listing_id, '{}');
  perform set_config('request.jwt.claim.sub', council, true);
  begin
    perform public.council_import(mixed, preview, true);
    raise exception 'Stale preview accepted';
  exception when raise_exception then if sqlerrm not like 'Listings changed.%' then raise; end if; end;
  assert not exists(select 1 from public.listings where fair_ref='FAIR-2'), 'Partial write from stale batch';
  preview := public.council_import(mixed);
  assert preview->0->>'status'='reserved';
  perform public.council_import(mixed, preview, true);
  assert (select reserved_by=visitor::uuid from public.listings where id=listing_id);

  perform public.apply_action(gen_random_uuid(), 'delete', listing_id, '{}');
  preview := public.council_import(rows);
  assert preview->0->>'status'='deleted';
  perform public.council_import(rows, preview, true);
  assert (select deleted from public.listings where id=listing_id);
  begin
    perform public.council_import(rows || rows);
    raise exception 'Duplicate ref accepted';
  exception when raise_exception then if sqlerrm <> 'Duplicate fair reference' then raise; end if; end;
  begin
    perform public.council_import(jsonb_set(rows,'{0,title}','""'));
    raise exception 'Invalid title accepted';
  exception when raise_exception then if sqlerrm <> 'Invalid listing in CSV' then raise; end if; end;
end $$;
rollback;
\echo 'PASS: council roles, read-only preview, confirmed updates, owner-scoped references, stale batches, reservations and deletions'
