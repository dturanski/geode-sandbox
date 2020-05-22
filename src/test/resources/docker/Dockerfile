FROM apachegeode/geode:1.12.0
RUN \
    gfsh -e 'start locator --name=Locator1 --hostname-for-clients=localhost' \
    gfsh -e 'connect' -e 'start server --name=Server1 --hostname-for-clients=localhost'
    gfsh -e 'connect' -e 'create region --name=myRegion --type=REPLICATE'
    		
CMD ["tail","-f","/dev/null"]