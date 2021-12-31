# winsome
a reWardINg SOcial Media
### TODO
- [ ] (FIX) Use a set instead of a list to keep recent people that commented/voted
  - That's because a user can comment n > 1 times in the update time interval
- [ ] After log in it is not possible to register, you must log out
- [ ] (FIX) prints

### DONE
- [X] Remote Procedure Calls to register new users
- [X] NIO + Thread Pool to execute request and send responses
- [X] login
- [X] logout
- [X] follow users
- [X] unfollow users
- [X] list users with tags in common
- [X] List following
- [X] List followers (locally by client)
  - [X] Pre-download followers list at login
  - [X] Each client need to keep a data structure to store followers (guarantee consistency)
- [X] RMI Callback to notify in/out coming followers
- [X] Publish posts
- [X] Show post given id
- [X] Rate post
- [X] Unregister for RMI Callback when user logs out
- [X] Show feed
- [X] View Blog
- [X] Delete post
  - [X] Delete rewins on it
- [X] Rewin a post
- [X] Add comments
- [X] Update Wallet values
  - [X] UDP Multicast to spread Wallet updates
- [X] Use URL to get a random seed in order to simulate BTC Trend
- [X] JSONs are rewritten after a certain delta time to reduce writing overhead
- [X] Soft server stop
- [X] Soft client stop
- [X] Configuration file parsing

### POLISH
- [ ] Divide server -> client output in reasonable quantity of pieces
  - [ ] In order to avoid string overflow
- [X] Some check args functions are repeated, It will be better to extract them to methods
- [ ] Put synchronized blocks in the right places
- [X] Make a better arrangement of responses
- [ ] JavaDoc