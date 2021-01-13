package kr.ac.kaist.jiset.spec

import kr.ac.kaist.ires.ir
import kr.ac.kaist.jiset.{ ECMA262_DIR, SPEC_HTML, LINE_SEP }
import kr.ac.kaist.jiset.util.Useful._
import org.jsoup._
import org.jsoup.nodes._
import scala.util.matching.Regex

// ECMASCript specifications
case class ECMAScript(grammar: Grammar, algos: List[Algo])

object ECMAScript {
  def parse(version: String): ECMAScript = {
    // read file content of spec.html
    val src = if (version == "") readFile(SPEC_HTML) else {
      val cur = currentVersion(ECMA262_DIR)
      changeVersion(version, ECMA262_DIR)
      val src = readFile(SPEC_HTML)
      changeVersion(cur, ECMA262_DIR)
      src
    }
    // source lines
    val lines = dropAppendix(src.split(LINE_SEP))
    // parse html
    val document = Jsoup.parse(src)
    // parse grammar
    val grammar = parseGrammar(lines, document)
    // parse algorithm
    val algos = parseAlgo(lines, grammar, document)
    // wrap grammar, algos
    ECMAScript(grammar, algos)
  }

  ////////////////////////////////////////////////////////////////////////////////
  // helper
  ////////////////////////////////////////////////////////////////////////////////
  def dropAppendix(lines: Array[String]): Array[String] = {
    val appendixLineNum = lines.indexOf("""<emu-annex id="sec-grammar-summary">""")
    if (appendixLineNum == -1)
      error("`ECMASCript.dropAppendix`: appendix not found")
    lines.slice(0, appendixLineNum)
  }

  // get ranges of mathed pattern from spec.html
  def getRanges(
    lines: Array[String],
    pattern: (Regex, Regex)
  ): Array[(Int, Int)] = {
    val (entryPattern, exitPattern) = pattern
    var rngs = Vector[(Int, Int)]()
    var entries = List[Int]()
    for ((line, i) <- lines.zipWithIndex) line match {
      case entryPattern() => entries ::= i + 1
      case exitPattern() =>
        if (!entries.isEmpty) {
          rngs :+= (entries.head, i)
          entries = entries.tail
        }
      case _ =>
    }
    rngs.toArray
  }

  def getChunks(
    lines: Array[String],
    pattern: (Regex, Regex)
  ): Array[List[String]] = {
    val rngs = getRanges(lines, pattern)
    val chunks = rngs.map { case (s, e) => lines.slice(s, e).toList }
    chunks
  }

  // check if inst has ??? or !!!
  def isComplete(inst: ir.Inst): Boolean = {
    var complete = true
    object Walker extends ir.UnitWalker {
      override def walk(expr: ir.Expr): Unit = expr match {
        case ir.ENotYetModeled(_) | ir.ENotSupported(_) => complete = false
        case _ => super.walk(expr)
      }
    }
    Walker.walk(inst)
    complete
  }

  ////////////////////////////////////////////////////////////////////////////////
  // grammar
  ////////////////////////////////////////////////////////////////////////////////
  // regexp for `emu-grammar` tagged elements
  val grammarPattern =
    ("""[ ]*<emu-grammar type="definition">""".r, "[ ]*</emu-grammar.*>".r)

  // parse spec.html to Grammar
  def parseGrammar(lines: Array[String], document: Document): Grammar = {
    // split codes by empty string
    // ex) split(List("a", "b", "", "c", "d")) = List(List("a", "b"), List("c", "d"))
    def split(prods: List[String]): List[List[String]] = {
      prods.span(_ != "") match {
        case (prod, _ :: remain) => prod :: split(remain)
        case (prod, Nil) => List(prod)
      }
    }

    // get lexical grammar list
    val lexElem = document.getElementById("sec-lexical-grammar")
    val lexProdElems = lexElem.getElementsByTag("emu-prodref").toArray(Array[Element]())
    val lexNames = lexProdElems.toList.map(_.attributes().get("name"))

    // codes for `emu-grammar` tagges elements
    val prodStrs = getChunks(lines, grammarPattern).toList.flatMap(split _)
    val prods = (for {
      prodStr <- prodStrs
      prod <- optional(Production(prodStr))
    } yield prod).toList

    // partition prods
    val (lexProds, nonLexProds) =
      prods.partition(p => lexNames.contains(p.lhs.name))

    // for debug
    //  (prodStrs zip prods).foreach {
    //    case (prodStr, prod) => {
    //      prodStr.foreach(println _)
    //      println(prod.lhs)
    //      prod.rhsList.foreach(println _)
    //    }
    //  }
    //  println(s"# of lexNames : ${lexNames.length}")

    println(s"# of lexical production: ${lexProds.length}")
    println(s"# of non-lexical production: ${nonLexProds.length}")

    Grammar(lexProds, nonLexProds)
  }

  ////////////////////////////////////////////////////////////////////////////////
  // algorithm
  ////////////////////////////////////////////////////////////////////////////////
  // regexp for `emu-alg` tagged elements
  val algoPattern = ("[ ]*<emu-alg.*>".r, "[ ]*</emu-alg.*>".r)

  // parse spec.html to Algo
  def parseAlgo(
    lines: Array[String],
    grammar: Grammar,
    document: Document
  ): List[Algo] = {
    // HTML elements with `emu-alg` tags
    val elems = document.getElementsByTag("emu-alg").toArray(Array[Element]())

    // codes for `emu-alg` tagged elements
    val codes = getChunks(lines, algoPattern)
    println(s"# `emu-alg` tagged elements: ${codes.size}")

    // algorithms
    val (atime, algos) = time((for {
      (elem, code) <- elems zip codes
      algo <- Algo(elem, code, grammar)
    } yield algo).toList)
    println(s"# algorithms: ${algos.length} ($atime ms)")

    // return algos
    algos
  }

  // pre-defined global identifiers
  val PREDEF = Set(
    // Completion-related ECMAScript internal algorithms
    "NormalCompletion", "ThrowCompletion", "ReturnIfAbrupt", "Completion",
    // ECMAScript type getter algorithm
    "Type",
    // JISET specific internal algorithms
    "IsAbruptCompletion", "WrapCompletion",
    // JISET specific global variables
    "GLOBAL_agent"
  )
}