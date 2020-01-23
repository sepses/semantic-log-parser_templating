for file in input/ottr/instances-*.stottr
do
  time java -jar input/lutra.jar --library input/ottr/templates.stottr --libraryFormat stottr --inputFormat stottr "$file" --mode expand --fetchMissing > "$file".ttl
done
