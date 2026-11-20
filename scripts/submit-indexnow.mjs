#!/usr/bin/env node
import { pingIndexNow } from "./ping-indexnow.mjs";

pingIndexNow()
  .then((res) => {
    console.log("⚡ INDEXNOW SUBMIT (scripts/submit-indexnow.mjs):");
    console.log(JSON.stringify(res, null, 2));
    if (!res.ok) process.exit(1);
  })
  .catch((err) => {
    console.error("❌ Erro no submit-indexnow:", err.message);
    process.exit(1);
  });
