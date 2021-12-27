import chisel3.stage.ChiselStage
import QuickDMA._
import chipsalliance.rocketchip.config.Parameters
object ANDemitVerilog extends App{
    implicit val p:Parameters = new QuickDMAConfig
      (new ChiselStage).emitVerilog(new Wrapper()(p),
        Array("--target-dir", "output/"))

}

