## Compile it yourself
Clone the repository to your local machine

    git clone https://github.com/kartben/kura-sigfox-gateway.git
    cd kura-sigfox-gateway

Set the KURA_WS environment variable, pointing at your local Eclipse workspace

    # Windows
    setx KURA_WS c:\Users\you\eclipse-workspace
    # Linux / OSX
    export KURA_WS=/Users/you/eclipse-workspace
 
Now build with Maven

    mvn clean verify    

After the compilation you will find the Deployment Package ready to be installed to a remote Kura!
