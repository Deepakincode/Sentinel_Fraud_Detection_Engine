# Sentinel Fraud Detection Engine

A real-time transaction risk-scoring backend built with Core Java, MySQL, and JDBC.
Detects suspicious transactions using rule-based logic, processes them concurrently
using an ExecutorService thread pool, enforces Role-Based Access Control (RBAC),
logs every action for audit purposes, and generates file-based reports.

## How to run

Requires JDK 17+ (tested on JDK 21). No external dependencies needed to run the demo.

```bash
# Compile
find src -name "*.java" > sources.txt
javac -d out @sources.txt

# Run the end-to-end demo (uses in-memory data, no DB setup needed)
java -cp out com.sentinel.Main

# Run the test suite
java -cp out com.sentinel.test.SentinelTest
```

Output files are written to `logs/audit.log` and `reports/suspicious_activity_report.txt`.

## Running against a real MySQL database (optional)

1. Run `sql/schema.sql` in MySQL to create the `sentinel_db` database and tables.
2. Add the MySQL Connector/J jar to your classpath.
3. Update the credentials in `src/com/sentinel/db/ConnectionPool.java`.
4. In `Main.java`, swap `InMemoryTransactionDAO`/`InMemoryUserDAO` for
   `TransactionDAOImpl`/`UserDAOImpl` — everything else stays exactly the same,
   because both pairs implement the same `TransactionDAO`/`UserDAO` interfaces
   (this is the whole point of the DAO pattern).

## Project structure

```
src/com/sentinel/
  model/        Transaction, User, Role  (plain data classes)
  db/           ConnectionPool           (JDBC connection pooling)
  dao/          TransactionDAO, UserDAO  (interfaces - DAO pattern)
                TransactionDAOImpl, UserDAOImpl        (real MySQL/JDBC versions)
                InMemoryTransactionDAO, InMemoryUserDAO (offline demo versions)
  service/      RiskScoringService   (the fraud-detection rules)
                TransactionProcessor (multithreading via ExecutorService)
                FraudDetectionService (RBAC-gated actions)
  security/     AccessControl        (RBAC enforcement)
  util/         AuditLogger, ReportGenerator
  test/         SentinelTest         (functional + integration tests)
  Main.java     Demo entry point
sql/schema.sql  MySQL table definitions + seed users
```

## How the fraud detection works (rule-based, not ML)

Every transaction gets a risk score from 0-100 based on three rules. If the total
score is 50 or more, it's flagged:

| Rule | Points | Example |
|---|---|---|
| High amount (>= ₹50,000) | 55 | A single ₹75,000 transaction |
| "Impossible travel" (same account, different city, within 5 min) | 50 | Mumbai then London 2 minutes later |
| Rapid-fire (3+ transactions from same account within 1 min) | 55 | Three small transactions 15 seconds apart |

## Why a thread pool (ExecutorService) instead of one thread?

If 1,000 transactions arrive, checking them one at a time is slow. A fixed thread
pool lets several transactions get risk-scored simultaneously — like having several
cashiers instead of one. The tricky part: two rules above need to look at a
transaction's *history* for the same account, so if two transactions from the same
account ran on different threads at the exact same time, one could finish before
the other's history was recorded — a race condition. The fix used here: transactions
for the SAME account are chained to run in order (via `CompletableFuture` chaining),
while transactions for DIFFERENT accounts still run fully in parallel. This keeps
results correct without giving up concurrency.

## RBAC roles

| Role | Can do |
|---|---|
| EMPLOYEE | View transactions only |
| MANAGER | View + Approve/Block flagged transactions |
| ADMIN | Everything, including viewing audit logs |

## Likely viva questions and how to answer

**Q: Is this using machine learning?**
No — this is rule-based risk scoring. I check each transaction against fixed
conditions (amount, location/time pattern, frequency) and sum up points. My
Ransomware Detection project is the one that uses a trained ML model instead.

**Q: Why did you use a connection pool instead of opening a new connection every time?**
Opening a new DB connection involves a TCP handshake and authentication every time,
which is slow if done for every query. The pool opens a fixed number of connections
once and reuses them, which is much faster under load.

**Q: What is the DAO pattern and why use it?**
DAO (Data Access Object) separates "how do I talk to the database" from "what does
my business logic do." The service layer only depends on the `TransactionDAO`
interface, not on JDBC/MySQL specifics — so I could swap in the in-memory version
for testing/demos without changing any other code.

**Q: What does ExecutorService actually do here?**
It manages a fixed pool of worker threads and hands each submitted transaction to
whichever thread is free, so many transactions get evaluated in parallel instead
of one after another.

**Q: What was the hardest part of this project?**
Handling the race condition described above — realizing that rules depending on
transaction history needed same-account ordering guarantees even while running
multiple threads in parallel, and fixing it with per-account future chaining
instead of naive parallel submission.

**Q: How did you test it?**
`SentinelTest.java` has functional tests (does a high-amount transaction get
flagged in isolation?) and an integration test (does the full pipeline —
concurrent processing, risk scoring, and persistence — work end-to-end together?).

**Q: What would you improve with more time?**
Move the rule thresholds into a config file instead of hardcoding them, add more
rules (e.g. spending pattern deviation from a rolling average), and add real
password-based authentication instead of the simplified username lookup used here.
