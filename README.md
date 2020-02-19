# semantic-log-parser-templating
Semantic approaches to parse log files

Program creates the instances.stottr

Then execute the following line to create the out.stottr from the templates.stottr
PS C:\Users\aekelhart\Documents\TU GIT\semantic-log-parser_templating\input> java -jar .\lutra.jar --library .\templates.stottr --libraryFormat stottr --inputFormat stottr .\instances.stottr --mode expand --fetchMissing > out.stottr

kern log: `java -jar ./input/lutra.jar --library ./input/various/kern/kern-templates.stottr --libraryFormat stottr --inputFormat stottr ./input/various/kern/kern-all-instances --mode expand --fetchMissing > ./input/various/kern/kern-out.ttl`

syslog: `java -jar ./input/lutra.jar --library ./input/various/syslog/syslog-templates.stottr --libraryFormat stottr --inputFormat stottr ./input/various/syslog/syslog-all-instances.ttl --mode expand --fetchMissing > ./input/various/syslog/syslog-out.ttl`

auth log: `java -jar ./input/lutra.jar --library ./input/various/auth/auth-templates.stottr --libraryFormat stottr --inputFormat stottr ./input/various/auth/auth-all-instances --mode expand --fetchMissing > ./input/various/auth/auth-out.ttl`
