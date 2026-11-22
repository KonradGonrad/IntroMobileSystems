import { createClient } from "npm:@supabase/supabase-js@2.49.8";
import { cert, initializeApp } from "npm:firebase-admin@13.4.0/app";
import { getMessaging } from "npm:firebase-admin@13.4.0/messaging";

const account = JSON.parse(Deno.env.get("FCM_SERVICE_ACCOUNT")!);
initializeApp({ credential: cert(account) });
const db = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);
const secret = Deno.env.get("PUSH_WEBHOOK_SECRET");

Deno.serve(async (request) => {
  if (request.method !== "POST" || !secret || request.headers.get("x-webhook-secret") !== secret) {
    return new Response(null, { status: 403 });
  }
  const hook = await request.json();
  if (hook.type !== "INSERT" || hook.schema !== "public" || hook.table !== "activity_events") {
    return new Response(null, { status: 400 });
  }
  const { data: event, error } = await db.from("activity_events").select("id,recipient,kind")
    .eq("id", hook.record.id).single();
  if (error || !event) return new Response(null, { status: 503 });
  const devices = await db.from("push_devices").select("token").eq("user_id", event.recipient);
  if (devices.error) return new Response(null, { status: 503 });
  const tokens = devices.data.map((row) => row.token as string);
  let retry = false;
  for (let start = 0; start < tokens.length; start += 500) {
    const batch = tokens.slice(start, start + 500);
    const sent = await getMessaging().sendEachForMulticast({
      tokens: batch, android: { priority: "high" },
      data: { recipient: event.recipient, kind: event.kind, event_id: event.id },
    });
    const stale: string[] = [];
    sent.responses.forEach((result, i) => {
      if (result.error?.code === "messaging/registration-token-not-registered") stale.push(batch[i]);
      else if (!result.success) retry = true;
    });
    if (stale.length) await db.from("push_devices").delete().eq("user_id", event.recipient).in("token", stale);
  }
  return new Response(null, { status: retry ? 503 : 204 });
});
