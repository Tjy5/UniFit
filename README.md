# UniFit

UniFit is an intelligent school uniform ordering platform with a user-facing mall,
an admin console, size recommendation workflows, order fulfillment, inventory
management, reviews, and analytics.

## Modules

- `School-uniform-intelligent-ordering-system-usersystem`: user backend and user web app.
- `School-uniform-intelligent-ordering-system-managementor`: admin backend and admin web app.
- `suios.sql`: public schema-only snapshot. It intentionally contains no runtime data.

## Public Repository Notes

This repository is prepared for public sharing. Local environment files, build
outputs, dependency folders, logs, and generated reports are ignored. The schema
snapshot is sanitized and does not include exported users, addresses, orders,
login logs, password hashes, or other runtime data.

Do not commit real `.env` files, database exports with data, upload folders, or
production credentials.

## Verification

Run the fast verification suite from the repository root:

```powershell
npm install
npm run verify
```

Full-chain E2E requires a prepared MySQL test database and explicit admin test
credentials through environment variables.
