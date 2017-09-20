Group Messenger with a Local Key-Value Table
----------------------------------------------
Step 1 : Writing a content provider(GroupMessengerProvider.java)
The content provider stores all the messages. The abstraction it provides should be a general key-value table.
We will use content provider as a table storage that stores(key,value) pairs where key is the file name and its contents are the messages.
The content provider has two columns:
1. first column is named as "key". This column is used to store all the keys
2. Second column is named as "value". This column is used to store all values.
3. All keys and valules that the provider stores are in string format.

OnPTestClickListener.java is used to test your content provider.

Step 2 : Implementing a Multicast(GroupMessengerActivity.java)
The app will multicast every user-entered message to all app instances(including the one that is sneding message)
The app will be able to send/receive multiple times
The app will be able to handle concurrent messages.
The app will assign a sequence number to each message it receives.
Each message along with its sequence number is stored as a key value pair in the content provider.
Key : sequence number
Value : Message

Requirements Document:
----------------------
PA 2 Part A Specification.pdf

Path to the main files:
-----------------------
DS-Project-2A/GroupMessenger1/app/src/main/java/edu/buffalo/cse/cse486586/groupmessenger1/

Name of the files :
---------------------
1) GroupMessengerActivity.java
2) GroupMessengerProvider.java
3) OnPTestClickListener.java
