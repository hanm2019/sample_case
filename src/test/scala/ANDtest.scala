
import QuickDMA.{QuickDMAConfig, Wrapper}
import chipsalliance.rocketchip.config.Config
import org.scalatest._
import chiseltest._
import chisel3._
import chiseltest.experimental.TestOptionBuilder._
import chiseltest.internal.WriteVcdAnnotation

class QuickDMA_test extends FlatSpec with ChiselScalatestTester with Matchers{
    behavior of "QuickDMAModule"
    // test class body here
    it should "read some number to tlb" in {
        //test case body here
        implicit val parames: Config = (new QuickDMAConfig).toInstance
        test(new Wrapper()(parames)).withAnnotations(Seq(WriteVcdAnnotation )) { c =>
          c.io.control.wen.poke(false.B)
          c.io.control.waddr.poke(0.B)
          c.io.control.wdata.poke(0.U)
          c.io.control.ren.poke(false.B)
          c.io.control.raddr.poke(0.U)

          c.clock.step()

          // set sample_num
          c.io.control.wen.poke(true.B)
          c.io.control.waddr.poke(1.U)
          c.io.control.wdata.poke(32.U)

          c.clock.step()

          // start
          c.io.control.wen.poke(true.B)
          c.io.control.waddr.poke(0.U)
          c.io.control.wdata.poke(1.U)
          c.clock.step()

          c.io.control.wen.poke(false.B)

          c.clock.step(60)

        }
    }
}