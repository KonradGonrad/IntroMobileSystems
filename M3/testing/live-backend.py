from pathlib import Path
import json,urllib.request,uuid,secrets
cfg=dict(l.split('=',1) for l in Path('local.properties').read_text().splitlines() if '=' in l and not l.startswith('#'))
base=cfg['SUPABASE_URL'].strip().rstrip('/');key=cfg['SUPABASE_ANON_KEY'].strip()
def req(path,data,token=None):
 h={'apikey':key,'Content-Type':'application/json'}
 if token:h['Authorization']='Bearer '+token
 r=urllib.request.urlopen(urllib.request.Request(base+path,data=json.dumps(data).encode(),headers=h),timeout=20)
 raw=r.read();return json.loads(raw) if raw else None
users=[]
for label in ['Owner','Buyer']:
 email='m3.test.'+uuid.uuid4().hex[:10]+'@edu.p.lodz.pl';password=secrets.token_urlsafe(20)
 s=req('/auth/v1/signup',{'email':email,'password':password,'data':{'display_name':'Demo '+label}})
 assert s.get('access_token'),'Signup did not return session'
 users.append(s);print('PASS signup '+label,flush=True)
 # Verify password-based login separately.
 assert req('/auth/v1/token?grant_type=password',{'email':email,'password':password}).get('access_token')
a,b=[u['access_token'] for u in users]
def rpc(name,data,t):return req('/rest/v1/rpc/'+name,data,t)
item=str(uuid.uuid4())
def action(kind,t,payload={}):return rpc('apply_action',{'action_id':str(uuid.uuid4()),'kind':kind,'target_id':item,'payload':payload},t)
action('save',a,{'title':'M3 integration test','description':'Synthetic listing created by automated integration check.','category':'BOOKS','condition':'GOOD'})
assert any(i['id']==item for i in rpc('feed_snapshot',{},b));print('PASS shared listing',flush=True)
action('reserve',b)
assert next(i for i in rpc('feed_snapshot',{},b) if i['id']==item)['reserved_by_me'];print('PASS reservation',flush=True)
c=rpc('start_conversation',{'listing_id':item},b)
rpc('apply_action',{'action_id':str(uuid.uuid4()),'kind':'message','target_id':str(uuid.uuid4()),'payload':{'conversation_id':c,'body':'Synthetic integration test message'}},b)
assert any(m['conversation_id']==c for m in rpc('chat_snapshot',{},a)['messages']);print('PASS shared chat',flush=True)
action('cancel',b);action('delete',a)
assert not any(i['id']==item for i in rpc('feed_snapshot',{},b));print('PASS cancellation and deletion',flush=True)
print('Two fictional Demo accounts and one archived test conversation remain in Supabase.')
