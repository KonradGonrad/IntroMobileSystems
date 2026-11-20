-- Run against the local test fixture or a disposable configured project, as postgres.
-- All fictional rows are rolled back. Never use real user accounts in this test.
begin;
insert into auth.users(id,email,raw_user_meta_data) values
 ('10000000-0000-0000-0000-000000000001','m3.owner@edu.p.lodz.pl','{"display_name":"Demo Owner"}'),
 ('10000000-0000-0000-0000-000000000002','m3.buyer@edu.p.lodz.pl','{"display_name":"Demo Buyer"}'),
 ('10000000-0000-0000-0000-000000000003','m3.other@edu.p.lodz.pl','{"display_name":"Demo Other"}');
do $$ begin
 begin
 insert into auth.users(id,email,raw_user_meta_data) values(gen_random_uuid(),'edu@gmail.com','{"display_name":"Wrong Domain"}');
 raise exception 'TEST FAILED: wrong email domain was accepted';
 exception when raise_exception then
 if sqlerrm like 'TEST FAILED%' then raise; end if;
 end;
end $$;
set local role authenticated;
select set_config('request.jwt.claim.sub','10000000-0000-0000-0000-000000000001',true);
select public.apply_action('20000000-0000-0000-0000-000000000001','save','30000000-0000-0000-0000-000000000001',
 '{"title":"Demo kettle","description":"Synthetic kettle for testing only","category":"KITCHEN","condition":"GOOD"}');
-- Owner cannot reserve own item.
do $$ begin
 begin
 perform public.apply_action(gen_random_uuid(),'reserve','30000000-0000-0000-0000-000000000001','{}');
 raise exception 'TEST FAILED: owner reserved own item';
 exception when raise_exception then if sqlerrm like 'TEST FAILED%' then raise; end if; end;
end $$;
select set_config('request.jwt.claim.sub','10000000-0000-0000-0000-000000000002',true);
select public.apply_action('20000000-0000-0000-0000-000000000002','reserve','30000000-0000-0000-0000-000000000001','{}');
-- Repeat is idempotent.
select public.apply_action('20000000-0000-0000-0000-000000000002','reserve','30000000-0000-0000-0000-000000000001','{}');
select public.start_conversation('30000000-0000-0000-0000-000000000001') as conversation_id \gset
select public.apply_action('20000000-0000-0000-0000-000000000003','message','40000000-0000-0000-0000-000000000001',
 jsonb_build_object('conversation_id', :'conversation_id', 'body','Synthetic test message'));
select public.apply_action('20000000-0000-0000-0000-000000000003','message','40000000-0000-0000-0000-000000000001',
 jsonb_build_object('conversation_id', :'conversation_id', 'body','Synthetic test message'));
do $$ begin
 if (select count(*) from public.messages)<>1 then raise exception 'TEST FAILED: duplicate message'; end if;
end $$;
select set_config('request.jwt.claim.sub','10000000-0000-0000-0000-000000000003',true);
do $$ begin
 if (select count(*) from public.messages)<>0 then raise exception 'TEST FAILED: private messages exposed'; end if;
 if jsonb_array_length(public.chat_snapshot()->'conversations')<>0 then raise exception 'TEST FAILED: private conversations exposed'; end if;
 begin
 perform public.apply_action(gen_random_uuid(),'reserve','30000000-0000-0000-0000-000000000001','{}');
 raise exception 'TEST FAILED: second reservation accepted';
 exception when raise_exception then if sqlerrm like 'TEST FAILED%' then raise; end if; end;
 begin
 perform public.apply_action(gen_random_uuid(),'cancel','30000000-0000-0000-0000-000000000001','{}');
 raise exception 'TEST FAILED: other user cancelled reservation';
 exception when raise_exception then if sqlerrm like 'TEST FAILED%' then raise; end if; end;
 begin
 perform public.apply_action(gen_random_uuid(),'delete','30000000-0000-0000-0000-000000000001','{}');
 raise exception 'TEST FAILED: other user deleted listing';
 exception when raise_exception then if sqlerrm like 'TEST FAILED%' then raise; end if; end;
end $$;
select set_config('request.jwt.claim.sub','10000000-0000-0000-0000-000000000001',true);
do $$ begin
 begin
 perform public.apply_action(gen_random_uuid(),'save','30000000-0000-0000-0000-000000000001',
 '{"title":"Changed kettle","description":"Synthetic updated kettle","category":"KITCHEN","condition":"GOOD"}');
 raise exception 'TEST FAILED: reserved listing was edited';
 exception when raise_exception then if sqlerrm like 'TEST FAILED%' then raise; end if; end;
end $$;
select public.apply_action('20000000-0000-0000-0000-000000000004','delete','30000000-0000-0000-0000-000000000001','{}');
do $$ begin
 if jsonb_array_length(public.feed_snapshot())<>0 then raise exception 'TEST FAILED: deleted listing in feed'; end if;
 if jsonb_array_length(public.chat_snapshot()->'messages')<>1 then raise exception 'TEST FAILED: conversation lost'; end if;
end $$;
rollback;
\echo 'PASS: university domain, ownership, reservation conflict, idempotency, chat isolation, soft deletion'
