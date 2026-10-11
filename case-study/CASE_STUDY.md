# Case Study Scenarios to discuss

## Scenario 1: Cost Allocation and Tracking
**Situation**: The company needs to track and allocate costs accurately across different Warehouses and Stores. The costs include labor, inventory, transportation, and overhead expenses.

**Task**: Discuss the challenges in accurately tracking and allocating costs in a fulfillment environment. Think about what are important considerations for this, what are previous experiences that you have you could related to this problem and elaborate some questions and considerations

**Questions you may have and considerations:**

Recording costs is the easy part. The difficult part is deciding where a cost belongs. Some
costs clearly belong to one warehouse, like its rent. Most are shared: one warehouse supplies
several stores, one truck delivers to three stores on the same route, and the same pickers work
on orders for different stores within the same hour. To split those costs you need allocation
keys, such as units handled, orders picked, pallet positions or kilometres driven, which is
basically activity-based costing. The key you pick decides which store looks expensive, so this
is an agreement between Finance and Operations as much as a technical question. I would keep the
rules visible and versioned, so a store manager can always see why a cost was charged to them.

Some other points I would keep in mind:
- Data arrives at different speeds and levels of detail. Warehouse scans are real time, payroll
  is monthly and carrier invoices can arrive weeks later, so the tool needs accruals and
  corrections, not only final figures.
- Inventory cost is more than the purchase price. Capital tied up, shrinkage, damage and
  obsolescence also count, and are usually allocated by product and time in stock.
- The cost centre key has to survive changes. In this system the Business Unit Code is the
  natural key, but it is reused when a warehouse is replaced. Cost records should therefore also
  point to the specific warehouse record, otherwise the old and the new warehouse get mixed.
- The allocated totals must reconcile with the general ledger. If they don't match what Finance
  booked, people stop trusting the tool.

A few things from my own work shaped how I look at this. At Lenskart I worked on the supply chain
system, where orders from the website, the apps and the stores were synced into the SCM system
by PHP cron jobs every 12 hours. We replaced those jobs with Kafka events, so every order arrived
as soon as it was placed, together with its channel and the warehouse handling it. Besides
helping cut delivery time from about 7 days to 3, it meant every step of an order could be traced
back to where it happened. That is the same foundation cost tracking needs: if every pick,
shipment and return is recorded as an event with its warehouse and store, costs can be assigned
per event instead of being estimated afterwards from a monthly batch.

In the same team we had a daily order report that kept listing orders that had already been
fixed. The root cause turned out to be manual corrections: the operations team was editing
orders directly in the database and only touched the parts of the order lifecycle they knew
about. We cleaned up the data, gave them a proper way to correct orders and added tests around
it. Since then I treat corrections as something the system has to record explicitly, which is
exactly what cost data needs to stay auditable.

Later, at an e-commerce company in Saudi Arabia, I worked on centralising prices across several
warehouses, with delivery charges calculated from the warehouse that actually shipped the order.
The same product had a different cost depending on where it came from. That is why I would
allocate costs along the real fulfilment path rather than spread them as an average.

Questions I would ask before starting:
- Who will use this, and for which decisions: store profitability, warehouse performance,
  network planning? That decides how detailed it needs to be.
- Which allocation keys does Finance already use, and do we have to follow the existing cost
  centre structure?
- Which source systems exist (warehouse, transport, payroll, ERP), and how reliable is their data?

## Scenario 2: Cost Optimization Strategies
**Situation**: The company wants to identify and implement cost optimization strategies for its fulfillment operations. The goal is to reduce overall costs without compromising service quality.

**Task**: Discuss potential cost optimization strategies for fulfillment operations and expected outcomes from that. How would you identify, prioritize and implement these strategies?

**Questions you may have and considerations:**

I would start by measuring before changing anything. With the allocation from Scenario 1 in
place it becomes clear which cost areas are large. In fulfilment these are usually labour and
transport, so that is where I would look first.

The strategies I would expect to evaluate:
- Supply each store from the nearest warehouse that has the stock, to reduce transport cost. The
  bonus task in this assignment (linking warehouses to products and stores) is a first step
  towards modelling this.
- Right-size the network. The sample data has a warehouse with capacity 100 holding 10 units,
  and unused capacity is paid for every month. Consolidating sites, or replacing one with a
  smaller warehouse through the replace flow, can bring real savings.
- Plan labour from the volume forecast, to reduce overtime and last-minute temporary staff,
  which are the most expensive hours.
- Place fast-moving products close to dispatch, so pickers walk less per order.
- Consolidate transport and plan routes so fewer trucks drive half empty.
- Review safety stock where demand is stable, to lower inventory carrying cost.

To protect service quality I would report every cost KPI together with a service KPI such as
on-time delivery, fill rate, lead time and damage rate. If costs go down while the fill rate
drops, the problem has only moved to the stores.

To prioritise, I would score each idea on expected impact, how confident we are in it and the
effort needed. The strongest ones I would pilot in one warehouse, compare the results with a
similar warehouse that did not change, and roll out only what actually improved the numbers. I
would not commit to savings percentages before that baseline exists.

Questions I would ask:
- Which service levels have been agreed with the stores, and which of them are fixed?
- What constraints do we have: carrier contracts, lease terms, labour agreements?
- Is there budget for investments such as automation, or should we focus on operational changes?

## Scenario 3: Integration with Financial Systems
**Situation**: The Cost Control Tool needs to integrate with existing financial systems to ensure accurate and timely cost data. The integration should support real-time data synchronization and reporting.

**Task**: Discuss the importance of integrating the Cost Control Tool with financial systems. What benefits the company would have from that and how would you ensure seamless integration and data synchronization?

**Questions you may have and considerations:**

Without an integration, the Cost Control Tool turns into one more spreadsheet. People retype
numbers, the figures drift away from what Finance booked, and meetings start with a discussion
about which number is correct. With an integration, operations and finance work with the same
figures, deviations are visible during the month instead of after the close, and the close
itself goes faster because less has to be reconciled by hand.

How I would make the integration reliable:
- Agree on master data first: who owns the mapping between Business Unit Codes and cost centres,
  GL accounts, currencies and periods. I would expect most integration issues to come from
  mismatched reference data rather than from the technical connection.
- Only send committed data. In Task 2 I changed the Store flow so the legacy system is called
  only after the database transaction has committed. For a finance integration I would go one
  step further and use a transactional outbox: the event is stored in the same transaction and
  published afterwards with retries, so nothing is lost when the finance system is unavailable.
- Make every message idempotent with a unique id, so a retry never books a cost twice. At
  RockWallet our payments go through Spreedly to providers like Worldpay and Checkout.com, with
  Kafka (AWS MSK) connecting the services, and duplicate and retried messages are something we
  have to handle every day when real money is involved.
- Never overwrite financial history. Corrections are booked as reversal entries, which keeps the
  audit trail complete.
- Reconcile automatically, with a daily job that compares totals on both sides and raises an
  alert when they differ.
- Monitor the integration: failed messages go to a dead-letter queue with alerting, and a health
  check shows when the synchronisation is falling behind.

I would also check what "real-time" really means for the business. Operational dashboards
benefit from near real-time data, but financial postings are usually made per period anyway, and
real-time everywhere is more expensive to build and run.

Questions I would ask:
- Which financial systems are involved, and which interfaces do they offer (API, files, messaging)?
- Which system is the source of truth for each type of data?
- What does real-time mean to the users: seconds, minutes or end of day?

## Scenario 4: Budgeting and Forecasting
**Situation**: The company needs to develop budgeting and forecasting capabilities for its fulfillment operations. The goal is to predict future costs and allocate resources effectively.

**Task**: Discuss the importance of budgeting and forecasting in fulfillment operations and what would you take into account designing a system to support accurate budgeting and forecasting?

**Questions you may have and considerations:**

Most fulfilment costs follow volume: more orders means more picking hours, more trucks and more
space. Forecasting therefore matters a lot. When a peak is visible in advance, staff and
transport can be arranged in time instead of paying for overtime and last-minute capacity. The
budget turns that plan into targets per business unit, so every warehouse knows what it is
accountable for.

What I would take into account in the design:
- A driver-based model instead of "last year plus a percentage". Costs are modelled as volume
  times a rate plus fixed costs, so when the volume forecast changes the cost forecast follows
  and stays explainable.
- The right inputs: sales forecasts per store and product, seasonality, promotions, store
  openings, and planned network changes such as a warehouse being replaced by a bigger one.
- Versions and scenarios. The approved budget stays fixed, a rolling forecast is updated every
  month, and what-if scenarios (for example closing a site) can be compared side by side.
- Budget, forecast and actuals per business unit and period, so deviations show up early.
- Controllers stay in charge. The model makes a proposal and people adjust it with a comment
  explaining why, which builds trust and keeps that knowledge in the system.
- Forecast accuracy is measured over time, so both the model and the assumptions improve.

Questions I would ask:
- What does the budget cycle look like today (annual plan, quarterly reforecast) and who owns it?
- How much historical data is available, and how reliable is it?
- At which level do people plan: per warehouse, per store, per product group?

## Scenario 5: Cost Control in Warehouse Replacement
**Situation**: The company is planning to replace an existing Warehouse with a new one. The new Warehouse will reuse the Business Unit Code of the old Warehouse. The old Warehouse will be archived, but its cost history must be preserved.

**Task**: Discuss the cost control aspects of replacing a Warehouse. Why is it important to preserve cost history and how this relates to keeping the new Warehouse operation within budget?

**Questions you may have and considerations:**

This scenario is close to what I implemented. A replacement does not overwrite anything: the
old warehouse gets an archivedAt date, a new record is created with the same Business Unit Code,
and both remain in the database. The stock of the new warehouse must also match the old one, so
no inventory value appears or disappears during the switch.

Keeping the history is important for several reasons:
- It is the baseline. The only way to know whether the new warehouse performs better, for
  example in cost per order or cost per unit stored, is to compare it with what the old one
  actually cost.
- It allows checking the business case. A replacement is usually approved because of expected
  savings, and without the old figures nobody can verify later whether they were achieved.
- Costs don't stop on the archive date. The old site can still have depreciation, lease
  obligations or clean-up costs that need to be booked somewhere.
- Financial records have to be retained for audit and legal reasons anyway.

To keep the new warehouse within budget I would focus on three things:
- Build its budget from real numbers: the old warehouse's actual costs, adjusted for the new
  capacity (in my test data TILBURG-001 goes from 30 to 40) and the assumptions in the business
  case.
- Track the transition costs separately. Moving stock, a period of double rent, hiring and IT
  setup are one-off costs. Mixed with normal operations they make the first months of the new
  warehouse look worse than they are, and left out they hide the real cost of the decision.
- Report on two levels. Per Business Unit Code you see the complete history of the business unit
  across replacements, and per warehouse record you can compare the old site with the new one.
  That is why cost records should carry the warehouse id as well as the code.

Questions I would ask:
- How long will both warehouses operate at the same time, and how is that overlap budgeted?
- Which savings were promised in the business case, and over what period?
- Should reports per Business Unit Code automatically include the history of archived
  warehouses, or should the user choose?

## Instructions for Candidates
Before starting the case study, read the [BRIEFING.md](BRIEFING.md) to quickly understand the domain, entities, business rules, and other relevant details.

**Analyze the Scenarios**: Carefully analyze each scenario and consider the tasks provided. To make informed decisions about the project's scope and ensure valuable outcomes, what key information would you seek to gather before defining the boundaries of the work? Your goal is to bridge technical aspects with business value, bringing a high level discussion; no need to deep dive.

Before defining the scope, I would want to understand three things: who the main users are
and which decisions the tool has to support, which systems hold the data today and who owns it,
and what a useful first version looks like (which costs, at what level of detail, and how up to
date). With those answers I would start small, for example labour and transport costs for one
warehouse, reconciled with the ledger. Once those numbers are trusted, the scope can grow.
