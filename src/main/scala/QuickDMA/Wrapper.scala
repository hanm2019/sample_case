package QuickDMA

import chisel3._
import chisel3.util.{Cat, Counter, Decoupled, DecoupledIO, Enum, is, log2Up, switch}
import freechips.rocketchip.config._

class Wrapper(implicit p:Parameters) extends Module
with QuickFPSParams {
  val io = IO(new Bundle() {
       val control = new WRIO(UInt(bitwidth.W),control_addr_width)
  })

  val CSR = RegInit(VecInit(Seq.fill(scala.math.pow(2,control_addr_width).toInt)(0.U(bitwidth.W))))

  val flag = Wire(UInt(bitwidth.W))
  val sample_num = Wire(UInt(bitwidth.W))

  flag := CSR(0)
  sample_num := CSR(1)

  when(io.control.wen){
    CSR(io.control.waddr) := io.control.wdata
  }

  io.control.rdata := CSR(io.control.raddr)

  val bucketAddrTable = SyncReadMem(max_bucket_num,UInt(bitwidth.W))

  val sIDLE::sSetupRead::sREAD::sRUN::Nil = Enum(4)

  val dma = Module(new Dma(1024, UInt(bitwidth.W), Some("/Users/hanmeng/Documents/research/sample_case/src/test/resource/linear-mem.txt")))
  val dma_io = Wire(new DmaIO(bitwidth))

  dma_io <> dma.io

  val state = RegInit(sIDLE)
  val dma_counter = Counter(1024)

  dma_io.writeControl.valid := false.B
  dma_io.writeControl.bits.length := 0.U
  dma_io.writeControl.bits.index := 0.U
  dma_io.writeControl.bits.size := DmaSize.word

  dma_io.writeChannel.valid := false.B
  dma_io.writeChannel.bits := 0.U

  dma_io.readControl.valid := false.B
  dma_io.readControl.bits.length := 0.U
  dma_io.readControl.bits.index := 0.U
  dma_io.readControl.bits.size := DmaSize.word

  dma_io.readChannel.ready := false.B

  switch(state){
    is (sIDLE){
      when(flag === 1.U(bitwidth.W)){
        state := sSetupRead
      }
    }
    is (sSetupRead){
      dma_io.readControl.valid := true.B
      dma_io.readControl.bits.index := 0.U
      dma_io.readControl.bits.length := 32.U
      when(dma_io.readControl.ready){
        state := sREAD
        dma_counter.reset()
      }
    }
    is(sREAD){
      when(dma_io.readChannel.valid){
        dma_io.readChannel.ready := true.B
        bucketAddrTable.write(dma_counter.value,dma_io.readChannel.bits)
        dma_counter.inc()
      }
      when(dma_counter.value === 32.U){
        state := sRUN
      }
    }
    is(sRUN){
      printf("finish!")
    }
  }
}
