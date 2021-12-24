# winsome
a reWardINg SOcial Media
### TODO
* Configuration file parsing 
* List following 
* List followers (locally by client)
* RMI Callback to notify in/out coming followers
  * Each client need to keep a data structure to store followers (guarantee consistency)
* After log in it is not possible to register, you must log out
* Other points...

### POLISH
* ReaderThread class has a lot of responsibilities, I have to refactor and make a Helper class
* Some check args functions are repeated, It will be better to extract them to methods
* It will be better if jsons are rewritten not after every data changes, but after a certain amount of time in order to reduce writing overhead
* Put synchronized blocks in the right places
* Make a better arrangement of responses

### DONE
* Remote Procedure Calls to register new users
* NIO + Thread Pool to execute request and send responses
* login
* logout
* follow users
* unfollow users
* list users with tags in common