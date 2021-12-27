test:
	sbt "testOnly MyModules.ANDtest"

testall:
	sbt test

verilog:
	sbt 'test:runMain MyModules.ANDemitVerilog'

clean:
	rm *.v *.fir *.anno.json