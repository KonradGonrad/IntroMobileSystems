-- Run with psql -v ON_ERROR_STOP=1 on the disposable backend fixture.
begin;
do $$
declare owner_id uuid:=gen_random_uuid(); buyer uuid:=gen_random_uuid(); other_buyer uuid:=gen_random_uuid();
 ids uuid[]:='{}'; target uuid; cid uuid; action uuid:=gen_random_uuid(); entry jsonb;
begin
 insert into auth.users(id,email,raw_user_meta_data) values
 (owner_id,'m31.owner@edu.p.lodz.pl','{"display_name":"Owner"}'),
 (buyer,'m31.buyer@edu.p.lodz.pl','{"display_name":"Sam"}'),
 (other_buyer,'m31.other@edu.p.lodz.pl','{"display_name":"Other"}');
 perform set_config('request.jwt.claim.sub',owner_id::text,true);
 for n in 1..4 loop
  target=gen_random_uuid(); ids=array_append(ids,target);
  perform public.apply_action(gen_random_uuid(),'save',target,'{"title":"Desk","description":"Demo desk for test","category":"FURNITURE","condition":"GOOD"}');
 end loop;
 perform set_config('request.jwt.claim.sub',buyer::text,true);
 for n in 1..3 loop perform public.apply_action(gen_random_uuid(),'reserve',ids[n],'{}'); end loop;
 begin
  perform public.apply_action(gen_random_uuid(),'reserve',ids[4],'{}');
  raise exception 'Fourth reservation accepted';
 exception when raise_exception then if sqlerrm <> 'You can have up to 3 active reservations' then raise; end if; end;
 select value into strict entry from jsonb_array_elements(public.feed_snapshot()) where value->>'id'=ids[1]::text;
 assert entry->>'reserver'='Sam' and (entry->>'expires')::bigint=(extract(epoch from now()+interval '24 hours')*1000)::bigint;
 perform public.apply_action(gen_random_uuid(),'cancel',ids[1],'{}');
 begin
  perform public.apply_action(gen_random_uuid(),'reserve',ids[1],'{}');
  raise exception 'Immediate re-reserve accepted';
 exception when raise_exception then if sqlerrm <> 'Please wait 24 hours before reserving this listing again' then raise; end if; end;
 perform public.apply_action(gen_random_uuid(),'reserve',ids[4],'{}');
 -- Simulate the deadline passing, leaving the recorded user in place.
 update public.listings set reserved_until=now()-interval '1 hour' where id=ids[2];
 perform public.feed_snapshot();
 assert (select reserved_by is null from public.listings where id=ids[2]);
 assert (select until_at=now()+interval '23 hours' from public.reservation_blocks where listing_id=ids[2] and user_id=buyer);
 perform public.apply_action(action,'like',ids[1],'{"liked":true}');
 perform public.apply_action(action,'like',ids[1],'{"liked":true}');
 cid=public.start_conversation(ids[1]);
 perform public.apply_action(gen_random_uuid(),'message',gen_random_uuid(),jsonb_build_object('conversation_id',cid,'body','Hello'));
 assert public.activity_snapshot()='[]'::jsonb;
 perform set_config('request.jwt.claim.sub',other_buyer::text,true);
 perform public.apply_action(gen_random_uuid(),'reserve',ids[2],'{}');
 assert public.activity_snapshot()='[]'::jsonb;
 perform set_config('request.jwt.claim.sub',owner_id::text,true);
 assert jsonb_array_length(public.activity_snapshot())=7;
 assert (select count(*) from public.activity_events where recipient=owner_id and kind='like')=1;
 assert (select count(*) from public.activity_events where recipient=owner_id and kind='message')=1;
 assert not has_function_privilege('authenticated','public.apply_action_m3(uuid,text,uuid,jsonb)','execute');
 assert not has_table_privilege('authenticated','public.push_devices','select');
end $$;
rollback;
\echo 'PASS: limit, nickname, expiry, cooldown, other buyers, event types, isolation and retries'
