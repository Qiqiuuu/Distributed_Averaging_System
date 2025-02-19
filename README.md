# Distributed Averaging System - DAS
Uni project that uses UDP  
Open it a few times with 2 arguments [port] [number]  
It will make the first Master and other Slaves  
Slaves will send their numbers to Master, and based on the number, Master will take proper action:  
0 - it will broadcast avg from memory to local network  
-1 - will broadcast received number and end program  
default - it will add number to memory  
