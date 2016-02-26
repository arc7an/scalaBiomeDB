import BioGraph.{DBNode, Node, XRef, Sequence, Rel, BioEntity}
package BioGraph {

import java.security.MessageDigest
import sun.awt.image.DataBufferNative
import sun.security.provider.MD5
import utilFunctions._

/**
  * created by artem on 11.02.16.
  */


trait GraphElement {

  def getProperties: Map[String, Any]

  def isNode: Boolean

  def isRel: Boolean

  def getId: BigInt

  //  type Strand = String

}


abstract class Node(
                     properties: Map[String, Any],
                     var id: BigInt = -1)
  extends GraphElement {

  def isNode = true

  def isRel = false

  def getProperties = properties

  def setProperties(newProperties: Map[String, Any]): Unit = properties ++ newProperties

  def getLabels: List[String]

  override def toString: String = getLabels.toString()

  def getId = id

  def setId(newId: BigInt): Unit = id = newId

  //  def outgoing: List[rel]
  //  def incoming: List[rel]
}

abstract class Rel(
                    id: BigInt = -1,
                    start: Node,
                    end: Node,
                    properties: Map[String, Any] = Map())
  extends GraphElement {

  def isNode = false

  def isRel = true

  def setProperties(newProperties: Map[String, Any]): Unit = properties ++ newProperties

  def getProperties = properties

  def startNode = start

  def endNode = end

  override def toString = start.toString + "-[:" + getLabel + "]->" + end.toString

  def getLabel: String

  def getId = id
}

trait BioEntity {

  def getName: String
}

trait DNA {

  //  def getCoordinates: Coordinates

}


trait CCP extends BioEntity {

  def getLength: Int

  def getType: CCPType.Value

  def getSource: SourceType.Value

  def getChromType: DNAType.Value
}

trait FunctionalRegion {}

object DNAType extends Enumeration {

  type ChromType = Value

  val circular, linear, unknown = Value

  override def toString = Value.toString
}

object CCPType extends Enumeration {

  type CCPType = Value

  val Chromosome, Contig, Plasmid = Value

  override def toString = Value.toString
}

object Strand extends Enumeration {

  val forward, reverse, unknown = Value

  override def toString = Value.toString
}

object SourceType extends Enumeration {

  type SourceType = Value

  val GenBank, MetaCyc, unknown = Value

  override def toString = Value.toString
}

object TaxonType extends Enumeration {

  val genus,
  species,
  no_rank,
  family,
  phylum,
  order,
  subspecies_class,
  subgenus,
  superphylum,
  species_group,
  subphylum, suborder,
  subclass,
  varietas,
  forma,
  species_subgroup,
  superkingdom,
  subfamily,
  tribe
  = Value

  override def toString = Value.toString
}


case class Coordinates(
                        start: Int,
                        end: Int,
                        strand: Strand.Value) {

  require (start < end, "Start coordinate cannot have bigger value than end coordinate!")

  def getStart = start

  def getEnd = end

  def getStrand = strand

  def equals(that: Coordinates) = that match {
    case that: Coordinates => this.getStrand == that.getStrand &&
      this.getStart == that.getStart &&
      this.getEnd == that.getEnd &&
      this.getClass == that.getClass
    case _ => false
  }

  def comesBefore(that: Coordinates) = that match{
    case that: Coordinates => this.getStrand == that.getStrand &&
      this.getStart > that.getStart &&
      this.getClass == that.getClass
    case _ => false
  }
}

case class Boundaries(
                       firstGene: Gene,
                       lastGene: Gene) {
  require(firstGene.getCoordinates.comesBefore(lastGene.getCoordinates),
    "Start coordinate cannot have bigger value than end coordinate!")

  require(firstGene.getCoordinates.getStrand.equals(lastGene.getCoordinates.getStrand),
    "Genes in the operon must be located on the same strand!")

  require(firstGene.getOrganism.equals(lastGene.getOrganism),
    "Genes in the operon must be located in the same organism!")

  def getFirstGene = firstGene

  def getLastGene = lastGene

  def getStrand = getFirstGene.getCoordinates.getStrand
}

case class DBNode(
                   name: String,
                   properties: Map[String, String] = Map(),
                   nodeId: BigInt = -1)
  extends Node(properties, nodeId) {

  def getLabels = List("DB")

  def getName = name

  def equals(that: DBNode): Boolean = this.getName == that.getName && this.getClass == that.getClass
}

case class XRef(xrefId: String,
                dbNode: DBNode,
                properties: Map[String, String] = Map(),
                nodeId: BigInt = -1)
  extends Node(properties, nodeId) {

  def getLabels = List("XRef")

  def getXRef = xrefId

  def getDB = dbNode

  def equals(that: XRef) = that match {
    case that: XRef => xrefId.toUpperCase == that.getXRef.toUpperCase && this.getClass == that.getClass
    case _ => false
  }
}

abstract class Feature(coordinates: Coordinates,
                       properties: Map[String, Any] = Map(),
                       ccp: CCP,
                       nodeId: BigInt = -1)
  extends Node(properties, nodeId) {

  def getCoordinates = coordinates

  def getLabels = List("Feature", "DNA")

  def next = throw new Exception("Not implemented yet!")

  def previous = throw new Exception("Not implemented yet!")

  def overlaps = throw new Exception("Not implemented yet!")

  def getCCP = ccp

  def equals(that: Feature) = that match {
    case that: Feature => this.getCCP == that.getCCP &&
      this.getCoordinates == that.getCoordinates &&
      this.getClass == that.getClass
    case _ => false
  }
}

case class Gene(
                 name: String,
                 coordinates: Coordinates,
                 ccp: CCP,
                 term: Term,
                 organism: Organism,
                 properties: Map[String, Any] = Map(),
                 nodeId: BigInt = -1)
  extends Feature(coordinates, properties, ccp, nodeId)
  with BioEntity
  with DNA {

  override def getLabels = List("BioEntity", "Feature", "Gene")

  def getName = name

  def getProduct = throw new Exception("Not implemented yet!")

  def controlledBy = throw new Exception("Not implemented yet!")

  def getStandardName = term

  def equals(that: Gene): Boolean = that match {
    case that: Gene => this.getCoordinates == that.getCoordinates &&
      this.getCCP == that.getCCP &&
      this.getClass == that.getClass
    case _ => false
  }

  def getOrganism = organism
}

case class Terminator(
                       coordinates: Coordinates,
                       ccp: CCP,
                       properties: Map[String, Any] = Map(),
                       nodeId: BigInt = -1)
  extends Feature(coordinates, properties, ccp, nodeId)
  with DNA {

  override def getLabels = List("Terminator", "Feature", "DNA")
}

case class Promoter(name: String,
                    coordinates: Coordinates,
                    ccp: CCP,
                    organism: Organism,
                    tss: Int,
                    term: Term,
                    properties: Map[String, Any] = Map(),
                    nodeId: BigInt = -1)
  extends Feature(coordinates, properties, ccp, nodeId)
  with FunctionalRegion
  with DNA {

  override def getLabels = List("Promoter", "BioEntity", "Feature", "DNA")

  def getName = name

  def getStandardName = term

  def getRegulationType = throw new Exception("Not implemented yet!")

  def getOrganism = organism
}

case class MiscFeature(coordinates: Coordinates,
                       ccp: CCP,
                       properties: Map[String, Any] = Map(),
                       nodeId: BigInt = -1)
  extends Feature(coordinates, properties, ccp, nodeId)
  with DNA {

  override def getLabels = List("Misc_feature", "Feature", "DNA")
}

case class MiscStructure(
                          coordinates: Coordinates,
                          ccp: CCP,
                          properties: Map[String, Any] = Map(),
                          nodeId: BigInt = -1)
  extends Feature(coordinates, properties, ccp, nodeId)
  with DNA {

  override def getLabels = List("Misc_structure", "Feature", "DNA")
}

case class MobileElement(
                          name: String,
                          coordinates: Coordinates,
                          ccp: CCP,
                          properties: Map[String, Any] = Map(),
                          nodeId: BigInt = -1)
  extends Feature(coordinates, properties, ccp, nodeId)
  with BioEntity
  with DNA {

  override def getLabels = List("Mobile_element", "Feature", "BioEntity", "DNA")

  def getName = name
}

case class Operon(
                   name: String,
                   boundaries: Boundaries,
                   term: Term,
                   organism: Organism,
                   properties: Map[String, Any] = Map(),
                   var tus: List[TU] = List(),
                   nodeId: BigInt = -1)
  extends Node(properties, nodeId)
  with BioEntity
  with DNA {
  //  def getCoordinates = List(properties("first_gene_position"), properties("last_gene_position "), properties("strand"))

  def getLabels = List("Operon", "BioEntity", "DNA")

  def getName = name

  def getOrganism = organism

  def getTUs = tus

  def nextOperon = throw new Exception("Not implemented yet!")

  def overlapedOperons = throw new Exception("Not implemented yet!")

  def getStandardName = term

  def addTU(tu: TU): Unit = {
    var newTus = List(tu) ::: tus
    tus = newTus
  }
}

case class TU(
               name: String,
               term: Term,
               operon: Operon,
               organism: Organism,
               composition: List[Feature],
               properties: Map[String, Any] = Map(),
               nodeId: BigInt = -1)
  extends Node(properties, nodeId)
  with BioEntity
  with DNA {

  def getLabels = List("TU", "BioEntity", "DNA")

  def getName = name

  def consistsOf = composition

  def getStandardName = term

  def participatesIn = throw new Exception("Not implemented yet!")

  def getOperon = operon

  def getOrganism = organism
}

case class Chromosome(
                       name: String,
                       source: SourceType.Value = SourceType.unknown,
                       dnaType: DNAType.Value = DNAType.unknown,
                       organism: Organism,
                       length: Int = -1,
                       properties: Map[String, Any] = Map(),
                       nodeId: BigInt = -1)
  extends Node(properties, nodeId)
  with CCP {

  def getLength = length

  def getChromType = dnaType

  def getType = CCPType.Chromosome

  def getSource = source

  def getName = name

  def getOrganism = organism

  def getLabels = List("Chromosome", "BioEntity")

}

case class Plasmid(
                    name: String,
                    source: SourceType.Value = SourceType.unknown,
                    dnaType: DNAType.Value = DNAType.unknown,
                    organism: Organism,
                    length: Int = -1,
                    properties: Map[String, Any] = Map(),
                    nodeId: BigInt = -1)
  extends Node(properties, nodeId)
  with CCP {

  def getLength = length

  def getChromType = dnaType

  def getType = CCPType.Plasmid

  def getSource = source

  def getName = name

  def getLabels = List("Plasmid", "BioEntity")

  def getOrganism = organism

}

case class Contig(
                   name: String,
                   source: SourceType.Value = SourceType.unknown,
                   dnaType: DNAType.Value = DNAType.unknown,
                   organism: Organism,
                   length: Int = -1,
                   properties: Map[String, Any] = Map(),
                   nodeId: BigInt = -1)
  extends Node(properties, nodeId)
  with CCP {

  def getLength = length

  def getChromType = dnaType

  def getType = CCPType.Contig

  def getSource = source

  def getName = name

  def getLabels = List("Contig", "BioEntity")

  def getOrganism = organism

}

case class Term(
                 text: String,
                 nodeId: Int = -1)
  extends Node(Map(), nodeId) {

  def getText = text

  def getLabels = List("Term")

  def equals(that: Term) = that match {
    case that: Term => this.getText == that.getText && this.getClass == that.getClass
    case _ => false
  }

}

case class Organism(
                     name: String,
                     var taxon: Taxon = new Taxon("Empty", TaxonType.no_rank),
                     properties: Map[String, Any] = Map(),
                     nodeId: BigInt = -1)
  extends Node(properties, nodeId) {

  def getLabels = List("Organism")

  def getName = name

  def equals(that: Organism) = that match {
    case that: Organism => this.getName == that.getName && this.getClass == that.getClass
    case _ => false
  }

  def getTaxon = taxon

  def setTaxon(newTaxon: Taxon): Unit = taxon = newTaxon
}

case class Polypeptide(
                        name: String,
                        xRef: XRef,
                        sequence: Sequence,
                        term: Term,
                        organism: Organism,
                        properties: Map[String, Any] = Map(),
                        nodeId: Int = -1)
  extends Node(properties, nodeId)
  with BioEntity {

  def getName = name

  def getLabels = List("Polypeptide", "Peptide", "BioEntity")

  def getGene = throw new Exception("Not implemented yet!")

  def getOrganism = organism

  def getSeq = sequence

  def equals(that: Polypeptide) = that match {
    case that: Polypeptide => this.getSeq.equals(that.getSeq) &&
      this.getOrganism == that.getOrganism &&
      this.getName == that.getName &&
      this.getClass == that.getClass
    case _ => false
  }
}

case class Sequence(
                     sequence: String,
                     var md5: String = "",
                     var similarities: List[Sequence] = List(),
                     properties: Map[String, Any] = Map(),
                     nodeId: Int = -1)
  extends Node(properties, nodeId) {

  if (md5.length < 32) md5 = countMD5

  def getLabels = List("Sequence", "AA_Sequence")

  def getSequence = sequence

  def getMD5 = md5

  def countMD5 = utilFunctions.md5ToString(sequence)

  def equals(that: Sequence) = that match {
    case that: Sequence => this.getMD5 == that.getMD5 && this.getClass == that.getClass
    case _ => false
  }

  def addSimilarity(similarSequence: Sequence): Unit = {
    if (!similarities.contains(similarSequence)) {
      var newSimilarity = List(similarSequence) ::: similarities
      similarities = newSimilarity
      similarSequence.addSimilarity(this)
    }
  }
}

case class Taxon(
                  name: String,
                  taxonType: TaxonType.Value,
                  taxID: Int = -1,
                  nodeId: BigInt = -1)
  extends Node(properties = Map(), nodeId){

  def getLabels = List("Taxon")

  def getType = taxonType

  def getTaxID = taxID

  def getTaxonType = taxonType
}

case class Compound(
                   name: String,
                   inchi: String = "",
                   smiles: String = "",
                   var reference: List[XRef] = List(),
                   nodeId: BigInt = -1)
  extends Node(properties = Map(), nodeId)
  with BioEntity{

  def getLabels = List("Compound", "BioEntity")

  def getName = name

  def getInchi = inchi

  def getSmiles = smiles

  def getXrefs = reference

  def setXrefs(newXref: XRef): Unit = reference = List(newXref) ::: reference

  def equals(that: Compound): Boolean = that match {
    case that: Compound => this.getInchi == that.getInchi &&
      this.getClass == that.getClass
    case _ => false
  }
}

//case class Enzyme(
//                   name: String,
//                   var polypeptide: List[Polypeptide] = List(),
//                   var complex: List[Complex] = List(),
//                   var regulates: List[EnzymeRegulation] = List(),
//                   var catalizes: List[Reaction] = List(),
//                   nodeId: BigInt = -1)
//  extends Node(properties = Map(), nodeId)
//  with BioEntity{
//
//  def getLabels = List("Enzyme", "Protein", "BioEntity")
//
//  def getName = name
//
//  def getPolypeptide = polypeptide
//
//  def getComplexes = complex
//
//  def getRegulations = regulates
//
//  def getCatalization = catalizes
//
//  def setPolypeptide(newPolypeptide: Polypeptide) = polypeptide ::: List(newPolypeptide)
//
//  def setComplexes(listOfComplexes: List[Complex]): Unit = complex ::: listOfComplexes
//
//  def setRegulations(newRegulatesList: List[EnzymeRegulation]): Unit = regulates ::: newRegulatesList
//}
//
//case class Antiantitermintor(
//                            coordinates: Coordinates,
//                            ccp: CCP,
//                            var modulates: List[Terminator] = List(),
//                            var participatesIn: List[Attenuation],
//                            nodeId: BigInt = -1)
//  extends Feature(coordinates, properties = Map(), ccp, nodeId)
//  with DNA {
//
//  override def getLabels = List("Antiantitermintor", "Feature")
//
//  override def getCCP = ccp
//}

  class LinkTo(start: XRef, end: DBNode, properties: Map[String, String] = Map()) extends Rel(id = -1, start, end, properties) {

    def getLabel = "LINK_TO"

  }

  class Evidence(start: Node, end: XRef, properties: Map[String, String] = Map()) extends Rel(id = -1, start, end, properties) {

    def getLabel = "EVIDENCE"

  }

  case class Similar(start: Sequence, end: Sequence, identity: Float, evalue: Double, relId: BigInt = -1) extends Rel(relId, start, end, Map()) {

    def getLabel = "SIMILAR"

    def getStart = start

    def getEnd = end

  }


}
