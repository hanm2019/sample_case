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

//
//  val bucket_dma = RegInit(Bool(),false.B)
//
  val bucketAddrTable = SyncReadMem(max_bucket_num,UInt(bitwidth.W))
//  val bucketLenTable = SyncReadMem(max_bucket_num,UInt(bitwidth.W))
//
//  val frontEndTileIds = new Range(0,frontend_tile_num,1)
//
//  val ref_point = Reg(new Point())
//  val buckets_num = Vec(frontend_tile_num,Wire(UInt(log2Up(boundbox_lines_num).W)))
//  val init = RegInit(Bool(),false.B)
//  val soft_reset = RegInit(Bool(),false.B)
//  val travel = RegInit(Bool(),false.B)
//  val findmax = RegInit(Bool(),false.B)
//  val iterate = RegInit(Bool(),false.B)
//  val frontend_tile_dma = RegInit(Bool(),false.B)
//
//  val frontend_inited = Wire(UInt(frontend_tile_num.W))
//  val frontend_traveled = Wire(UInt(frontend_tile_num.W))
//  val frontend_foundmax = Wire(UInt(frontend_tile_num.W))
//
//  val FrontEndTiles = frontEndTileIds.map(frontEndId => Module(new FrontEndTile_rewrite(frontEndId)(p)))
//
//
//  frontEndTileIds.foreach(
//    idx => {
//      FrontEndTiles(idx).io.control.ref_point := ref_point
//      FrontEndTiles(idx).io.control.dma := frontend_tile_dma
//      FrontEndTiles(idx).io.control.init := init
//      FrontEndTiles(idx).io.control.findmax := findmax
//      FrontEndTiles(idx).io.control.travel := travel
//      FrontEndTiles(idx).io.control.soft_reset := soft_reset
//      FrontEndTiles(idx).io.control.iterate := iterate
//      FrontEndTiles(idx).io.control.bucket_num := buckets_num(idx)
//
//      frontend_inited(idx) := FrontEndTiles(idx).io.control.inited
//      frontend_traveled(idx) := FrontEndTiles(idx).io.control.traveled
//      frontend_foundmax(idx) := FrontEndTiles(idx).io.control.foundmax
//
//      buckets_num(idx) := CSR(idx + 2)
//    }
//  )
  val sIDLE::sSetupRead::sREAD::sRUN::Nil = Enum(4)

  val dma = Module(new Dma(1024, UInt(bitwidth.W), Some("src/test/resource/linear-mem.txt")))
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
