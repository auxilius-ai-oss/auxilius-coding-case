# Data sources: take-home case

Thanks for your interest in engineering at Auxilius. This repo holds a small working app with one
screen. The screen works, but the people who use it don't enjoy it. Your job is to change that.

## The user

Marta is a controls analyst in the finance team of a manufacturing group. Every quarter, controls
(automated checks such as "no vendor was paid twice") run against that quarter's ERP extracts.
Before they can run, Marta has to get the extracts into the app:

1. Create a new **version** of the SAP data source for the quarter.
2. Upload about 35 CSV and Excel extracts that IT exports for her.
3. Make sure every file was read correctly: every column has a usable name and the right type.
4. **Lock** the version. Locking loads every file into the database the controls read from. When
   it succeeds, the version becomes **current** and controls switch to it.

It is the Q4 2026 close. IT's extracts are in `sample-uploads/2026-q4`. Halfway through the
close, finance sent corrected re-exports of three of them, which are in
`sample-uploads/2026-q4-corrections`. Last quarter's version, `Q3 2026`, is locked and current.

## What Marta says

> "Every quarter I set the same column types again. The vendor IDs, the posting dates, all of
> it. Last quarter's version already has them right."

> "I press lock, get a coffee, come back, and it's failed. Then I'm reading a wall of text
> trying to work out which file it means."

> "Thirty-odd files, and I open each one to check it. Click, look, close. Click, look, close."

> "Finance sent three fixed files. I tried to upload them and it refused the whole lot because
> the names already exist. So it's delete, confirm, delete, confirm, upload."

> "Uploads just fail sometimes, and I don't always notice which ones."

> "I never know when I'm done. Is it ready to lock or not?"

> "I once deleted the wrong file and had to dig it out of my email to upload it again."

## Your task

Make this screen work for Marta. Decide what to change and what to leave alone. You can change
anything: the frontend, the API, the data model. Nothing about the current API or UI has to stay
as it is.

The API behaves like production. Uploads are slow and sometimes fail. Working out a file's schema
takes a few seconds and sometimes fails. Locking takes about a minute and runs one version at a
time. Don't remove this behaviour or turn it off in the settings. Your solution has to work with it.

## Running it

```sh
docker compose up
```

Then open http://localhost:5173. The first start takes a few minutes while images build and
dependencies install. After a backend change, run `docker compose up --build`.

To run it without Docker, you need Java 25 and Node 24:

```sh
cd backend && ./gradlew bootRun     # API on :8080
cd frontend && npm install && npm run dev   # UI on :5173, proxies /api to :8080
```

The database is in memory and is re-seeded on every start, so a restart gives you a clean slate.

| Path | What it is |
|---|---|
| `backend/` | Spring Boot 4, Java 25, H2 in memory. Seed data is in `seed/`, behaviour settings in `application.yml`. |
| `frontend/` | React 19, TypeScript, Vite, MUI, TanStack Query. The screen is in `src/datasets/`. |
| `sample-uploads/` | The files for the task. `./gradlew generateSampleUploads` regenerates them. |

Checks: `./gradlew test` in `backend/`; `npm test`, `npm run lint` and `npm run build` in
`frontend/`.

## What we look at

We look at the flow: how quickly and safely Marta gets from a folder of files to a locked
version, and what happens when things go wrong along the way. Visual polish is not graded. MUI is
there so you don't have to think about styling, and the default look is fine.

## AI tools

Use whatever tools you normally use, coding agents included. We have run this brief through
coding agents ourselves. A submission that looks like what an agent produces on its own will not
pass. We are hiring for your judgement about what Marta needs, so make sure it shows.

## Effort

There is no deadline. We expect this to take around four to six hours. If you go well over that,
stop and write down what you would do next.

## Submitting

1. Create a **private** repository from this template ("Use this template", then "Private").
   Please don't fork it or open a pull request here; those are public, and later applicants
   could see your work.
2. Commit your work to it.
3. Add a `WRITEUP.md` (a page is plenty) covering:
   - what you changed
   - what you deliberately left alone
   - why
4. Record a screen recording of at most two minutes showing Marta's flow with your changes. Link
   it from `WRITEUP.md`, or commit it.
5. Invite TODO-reviewer-handles as collaborators and email TODO-contact to let us know.

After we review it, we'll schedule a 30-minute call. You'll walk us through your solution, and then
we'll think through a variation of the problem together.
