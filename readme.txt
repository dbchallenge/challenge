The project ships with gradle wrapper, no gradle installation required.

1. Build and run

In order to build and start the application run from the project root

build the projet
./gradlew build

starting the application
./gradlew bootRun

If you have curl installed you can test the application running the following commands

create accounts			
curl -i -X POST -H "Content-Type: application/json" -d "{\"accountId\":\"Id-123\",\"balance\":1000}" http://localhost:18080/v1/accounts
curl -i -X POST -H "Content-Type: application/json" -d "{\"accountId\":\"Id-124\",\"balance\":1000}" http://localhost:18080/v1/accounts

retrieve accounts
curl -i -H "Accept: application/json" "http://localhost:18080/v1/accounts/Id-123"
curl -i -H "Accept: application/json" "http://localhost:18080/v1/accounts/Id-124" 

transfer money
curl -i -X POST -H "Content-Type: application/json" -d "{\"accountFrom\":\"Id-123\",\"accountTo\":\"Id-124\",\"amount\":500.00}" http://localhost:18080/v1/transfer

check account balances
curl -i -H "Accept: application/json" "http://localhost:18080/v1/accounts/Id-123"
curl -i -H "Accept: application/json" "http://localhost:18080/v1/accounts/Id-124" 

2. Implementation

- Concurrency
  The AccountsRepository is set up as a ConcurrentHashMap which supports atomic setters but does not span over modifications across 
  two entries. So my implementation relies on ReentrantLocks which creates some kind of a pseudo transaction. As a backdraft the lock 
  inside the account class makes it a little bit ugly. This means refactoring to make this happen outside the domain class.
  There is still a blind spot if within the transaction occurs an InterruptedException after changing one account balance. 
  In order to tackle this problem a control and compensation mechanism has to be implemented to make it production ready. In general I would 
  rather rely on a transactional backend that already supports the unit of work paradigm. Another problem is that from outside the 
  TransactionService it is possible to remove an account which may lead to additional problems.
  
- Authorisation
  The current implementation allows everybody to transfer money from one account to another without any permission checks.
  