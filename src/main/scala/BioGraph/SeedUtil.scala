package BioGraph

import java.io.File
import org.neo4j.graphdb.Node
import org.apache.logging.log4j.LogManager
import org.neo4j.graphdb.DynamicLabel
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import utilFunctions.{BiomeDBRelations, TransactionSupport}
import scala.collection.JavaConverters._

import scala.io.Source

/**
  * Created by artem on 09.06.16.
  */
class SeedUtil(seedFile: File, dataBaseFile: File) extends TransactionSupport {

  val logger = LogManager.getLogger(this.getClass.getName)
  logger.info("SeedUtil class")
  logger.info("Start processing " + seedFile.getName)

  val graphDataBaseConnection = new GraphDatabaseFactory().newEmbeddedDatabase(dataBaseFile)
  val fileReader = Source.fromFile(seedFile.getAbsolutePath)
  val linesOfSeedFile = fileReader.getLines().toList.drop(1)
  fileReader.close()
  val accession = linesOfSeedFile(1).split("\t")(0)

  def createSeedXrefs()  = transaction(graphDataBaseConnection) {

    val seedDBNode = DBNode("SEED").upload(graphDataBaseConnection)
    val organismNode = graphDataBaseConnection.findNode(DynamicLabel.label("Organism"), "accession", accession)

    def processOneSeedLine(lineInSeedFile: String): Unit = {
      val records = lineInSeedFile.split("\t")
      val geneCoordinates = new Coordinates(records(3).toInt, records(4).toInt, getStrand(records(6)))
      val query = "START org=node(" + organismNode.getId + ") " +
        "MATCH (org)<-[:PART_OF]-(g:Gene{start:" + geneCoordinates.start +
        ", end:" + geneCoordinates.end +
        ", strand:'" + geneCoordinates.getStrand + "'}) " +
        "RETURN ID(g)"
      val geneNode = graphDataBaseConnection.execute(query).asScala.toList


      def createSeedXref(geneNode: org.neo4j.graphdb.Node): Unit = {
        val xrefNode = graphDataBaseConnection.createNode(DynamicLabel.label("XRef"))
        xrefNode.setProperty("id", records(8).split(";Name=")(0).split("=")(1))

        geneNode.createRelationshipTo(xrefNode, BiomeDBRelations.evidence)
        xrefNode.createRelationshipTo(seedDBNode, BiomeDBRelations.linkTo)

        val func = records(8).split(";Name=")(1)
        val testingNode = graphDataBaseConnection.createNode(DynamicLabel.label("Function"))
        testingNode.setProperty("function", func)
        geneNode.createRelationshipTo(testingNode, BiomeDBRelations.isA)
      }

//      check how many gene are found
      if (geneNode.length == 1) {
        val geneNodeID = geneNode.head.get("ID(g)").toString.toLong
        createSeedXref(graphDataBaseConnection.getNodeById(geneNodeID))
      }
      else if (geneNode.isEmpty) logger.error("Gene not found with coordinates: " + geneCoordinates)
      else logger.error("Multiple genes: " + geneCoordinates)
    }

//    read and process each line of the SEED
    linesOfSeedFile.foreach(processOneSeedLine)
  }

  private def getStrand(record: String): Strand.Value = record match {
    case "+" => Strand.forward
    case "-" => Strand.reverse
    case _ =>
      logger.error("Unknown strand: " + record)
      Strand.unknown
  }

}
