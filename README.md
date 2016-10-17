# Members
* Line Pouvaret
* Mickaël Turnel

# Installation
4 jars are available
* JvnCoord.jar : the coordinator
* Irc.jar : the chat example
* Irc2.jar : the chat example with dynamic proxy implementation
* Burst.jar : the burst extension

Note: `JvnCoord` will create files ending with _.ser_. Delete those if you want to remove all cache saves.

# Usage
First launch the coordinator : `java -jar JvnCoord.jar`

Then either launch Irc :
* Irc (without proxy) : `java -jar Irc.jar`
* Irc (with proxy) : `java -jar Irc2.jar`

(You can use Irc with Irc2 at the same time)

Or Burst mode : `java -jar Burst.jar <nb_threads> <nb_test_per_thread>`

# Extension
* Client crash : The coordinator will remove a client if he can't reach it
* Server crash : The coordinator will save its state all along the execution and reload it if restarted
* Burst mode : Several threads will start and try to ReadLock/WriteLock a certain number of times
