
import QuickDMA.{QuickDMAConfig, Wrapper}
import chipsalliance.rocketchip.config.Config
import org.scalatest._
import chiseltest._
import chisel3._
import chiseltest.experimental.TestOptionBuilder._
import chiseltest.internal.WriteVcdAnnotation

import scala.io.Source

//
//class QuickFPS_test extends FlatSpec with ChiselScalatestTester with Matchers{
//    behavior of "QuickFPSModule"
//    // test class body here
//    it should "read some number to tlb" in {
//        //test case body here
//        implicit val parames: Config = (new QuickFPSConfig).toInstance
//        val bucket_address = new Array[Int](1024)
//        val bucket_length = new Array[Int](1024)
//        val upboundary = Array.ofDim[Int](1024,3)
//        val downboundary = Array.ofDim[Int](1024,3)
//
//        val bucket_num = 32
//        val sample_num = 64
//        var line_counter = 0
//        Source.fromFile("src/test/resource/test1.txt").getLines().foreach(
//          line =>{
//            val words = line.split(" ")
//            bucket_address(line_counter) = words(0).toInt
//            bucket_length(line_counter) = words(1).toInt
//            upboundary(line_counter)(0) = words(2).toInt
//            upboundary(line_counter)(1) = words(3).toInt
//            upboundary(line_counter)(2) = words(4).toInt
//            downboundary(line_counter)(0) = words(5).toInt
//            downboundary(line_counter)(1) = words(6).toInt
//            downboundary(line_counter)(2) = words(7).toInt
//            line_counter = line_counter + 1
//          }
//        )
//        require(line_counter >= bucket_num)
//        test(new Wrapper()(parames)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
//          c.io.control.wen.poke(false.B)
//          c.io.control.waddr.poke(0.B)
//          c.io.control.wdata.poke(0.U)
//          c.io.control.ren.poke(false.B)
//          c.io.control.raddr.poke(0.U)
//
//          c.io.valid.poke(true.B)
//
//          c.io.mem.rdata.poke(0.U)
//
//          c.clock.step()
//
//          // set bucket_num
//          c.io.control.wen.poke(true.B)
//          c.io.control.waddr.poke(1.U)
//          c.io.control.wdata.poke(bucket_num.U)
//
//          c.clock.step()
//
//          // set sample_num
//          c.io.control.wen.poke(true.B)
//          c.io.control.waddr.poke(2.U)
//          c.io.control.wdata.poke(sample_num.U)
//
//          c.clock.step()
//          // start
//          c.io.control.wen.poke(true.B)
//          c.io.control.waddr.poke(0.U)
//          c.io.control.wdata.poke(1.U)
//
//          c.clock.step(3)
//
//          for(clock_num <- 0 until bucket_num*4){
//            if(c.io.mem.ren.peek().litToBoolean){
//              c.io.valid.poke(false.B)
//              c.clock.step(4)
//              val address = c.io.mem.raddr.peek().litValue().toInt
//              if(address % 4 == 3){
//                val data = bucket_address(address / 4) + (bucket_length(address / 4).toLong << 32)
//                c.io.mem.rdata.poke(data.U)
//              } else {
//                val data = downboundary(address/4)(address % 4) + (upboundary(address / 4)(address % 4).toLong << 32)
//                c.io.mem.rdata.poke(data.U)
//              }
//              c.io.valid.poke(true.B)
//              c.clock.step()
//            }
//          }
//
//        }
//    }
//}
//
//

//
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