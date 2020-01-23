## semantic-log-parser-templating

Semantic approaches to parse log files

Program creates the instances.stottr

Then execute the following line to create the out.stottr from the templates.stottr

### WINDOWS 

RUN: `PS C:\Users\aekelhart\Documents\TU GIT\semantic-log-parser_templating\input> java -jar .\lutra.jar --library .\templates.stottr --libraryFormat stottr --inputFormat stottr .\instances.stottr --mode expand --fetchMissing > out.stottr`

afterwards, run `./ottr.sh` (is it possible to run .sh from windows?)

### LINUX/UNIX

RUN: `java -jar input/lutra.jar --library input/ottr/templates.stottr --libraryFormat stottr --inputFormat stottr input/ottr/instances.stottr --mode expand --fetchMissing > input/ottr/out.ttl`

afterwards, run `./ottr.sh`
