# winsome
a reWardINg SOcial Media
### TODO
* Configuration file parsing
* After log in it is not possible to register, you must log out
* Set socket timeout
* Fix prints
* Soft client stop
* Soft server stop
* Other points...

### POLISH
* Divide server -> client output in reasonable quantity of pieces
  * In order to avoid string overflow
* Refactor SignInService it has too responsibilities
* ReaderThread class has a lot of responsibilities, I have to refactor and make a Helper class
* Some check args functions are repeated, It will be better to extract them to methods
* It will be better if jsons are rewritten not after every data changes, but after a certain amount of time in order to reduce writing overhead
* Put synchronized blocks in the right places
* Make a better arrangement of responses
* Refactor Client's class
* JavaDoc

### DONE
* Remote Procedure Calls to register new users
* NIO + Thread Pool to execute request and send responses
* login
* logout
* follow users
* unfollow users
* list users with tags in common
* List following
* List followers (locally by client)
  * Pre-download followers list at login 
  * Each client need to keep a data structure to store followers (guarantee consistency)
* RMI Callback to notify in/out coming followers
* Publish posts
* Show post given id
* Rate post
* Unregister for RMI Callback when user logs out
* Show feed