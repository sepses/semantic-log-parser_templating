# semantic-log-parser-templating
Semantic approaches to parse log files

Program creates the instances.stottr

Then execute the following line to create the out.stottr from the templates.stottr
PS C:\Users\aekelhart\Documents\TU GIT\semantic-log-parser_templating\input> java -jar .\lutra.jar --library .\templates.stottr --libraryFormat stottr --inputFormat stottr .\instances.stottr --mode expand --fetchMissing > out.stottr