import fs from "node:fs";

const token = "sbp_v0_d61bbc0f218c0c8ae5d530431302f765df7d0815";
const projectRef = "usmqtnlsaavxqguwrtix";

const sql = `
CREATE TABLE IF NOT EXISTS public.orchestrator_state (
  id SERIAL PRIMARY KEY,
  goal TEXT NOT NULL,
  status TEXT NOT NULL DEFAULT 'RUNNING',
  step_count INT NOT NULL DEFAULT 0,
  max_steps INT NOT NULL DEFAULT 50,
  last_thought TEXT,
  last_action TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.orchestrator_tasks (
  id SERIAL PRIMARY KEY,
  state_id INT REFERENCES public.orchestrator_state(id) ON DELETE CASCADE,
  title TEXT NOT NULL,
  target TEXT NOT NULL DEFAULT 'CLOUD_ACTIONS',
  payload JSONB NOT NULL DEFAULT '{}'::jsonb,
  status TEXT NOT NULL DEFAULT 'PENDING',
  result JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.orchestrator_logs (
  id SERIAL PRIMARY KEY,
  state_id INT REFERENCES public.orchestrator_state(id) ON DELETE CASCADE,
  step INT NOT NULL,
  thought TEXT,
  action TEXT,
  raw_model_response JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE public.orchestrator_state ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.orchestrator_tasks ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.orchestrator_logs ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'Allow all for anon and service on state') THEN
    CREATE POLICY "Allow all for anon and service on state" ON public.orchestrator_state FOR ALL TO public USING (true) WITH CHECK (true);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'Allow all for anon and service on tasks') THEN
    CREATE POLICY "Allow all for anon and service on tasks" ON public.orchestrator_tasks FOR ALL TO public USING (true) WITH CHECK (true);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'Allow all for anon and service on logs') THEN
    CREATE POLICY "Allow all for anon and service on logs" ON public.orchestrator_logs FOR ALL TO public USING (true) WITH CHECK (true);
  END IF;
END
$$;
`;

async function main() {
  const res = await fetch(`https://api.supabase.com/v1/projects/${projectRef}/database/query`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ query: sql }),
  });

  if (!res.ok) {
    throw new Error(`HTTP ${res.status}: ${await res.text()}`);
  }
  const result = await res.json();
  console.log("Database schema created successfully:", result);
}

main().catch((err) => {
  console.error("Error creating schema:", err);
  process.exit(1);
});
