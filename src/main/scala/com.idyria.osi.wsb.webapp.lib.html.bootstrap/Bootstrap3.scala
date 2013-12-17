package com.idyria.osi.wsb.webapp.lib.html.bootstrap

import com.idyria.osi.vui.impl.html.HtmlTreeBuilder
import com.idyria.osi.vui.core.components.scenegraph.SGNode
import com.idyria.osi.wsb.webapp.view.WebappHTMLBuilder
import com.idyria.osi.vui.core.components.scenegraph.SGGroup
import com.idyria.osi.vui.impl.html.components.HTMLNode
import com.idyria.osi.vui.impl.html.components.Div
import com.idyria.osi.wsb.webapp.WebApplication
import com.idyria.osi.wsb.webapp.navigation.Group
import com.idyria.osi.vui.impl.html.components.HTMLTextNode
import com.idyria.osi.wsb.webapp.navigation.View
import com.idyria.osi.vui.impl.html.components.Table

/**
 * Inject points
 */
class TopNavbar extends Div with HtmlTreeBuilder {

  type Self = TopNavbar

  def and(cl: TopNavbar ⇒ Unit): TopNavbar = {
    cl(this)
    this
  }

  def header(cl: ⇒ HTMLNode) {

    this.searchByName("header") match {
      case Some(h) ⇒ h.@->("content", cl)
      case None    ⇒
    }

  }

  /**
   * Creates Navbar Menus from Application Navigation, and add them to navbar placeholder
   */
  def menusFromNavigation(app: WebApplication) = {

    // Create Menus
    //-------------------

    //-- Base recursive function
    def navElementToNode(elt: Any): HTMLNode = {

      elt match {

        //-- Group
        //----------------
        case g: Group ⇒

          // Create Group as List item
          //------------
          //var groupItem = li

          // Group View id is specified in attribute
          //--------
          var groupLink = g.view match {
            case null ⇒ "#"
            case v    ⇒ g.fullPath + "/" + v
          }

          // Do Sub Content
          //---------
          var groupContent = (g.views.size + g.groups.size) match {

            case 0 ⇒ ""
            case _ ⇒

              ""
            // List(g.views.map(v => navElementToString(v)).mkString,g.groups.map(navElementToString(_)).mkString).mkString

          }
          // Gather in this group content
          //--------
          li {
            // Link
            a(g.name, groupLink)

            // Content
            (g.views.size + g.groups.size) match {

              // No content
              case 0 ⇒

              // Content -> do 
              case _ ⇒

                g.views.map(v ⇒ navElementToNode(v)).foreach {
                  elt ⇒ add(elt)
                }
                g.groups.map(v ⇒ navElementToNode(v)).foreach {
                  elt ⇒ add(elt)
                }

              // List(g.views.map(v => navElementToString(v)).mkString,g.groups.map(navElementToString(_)).mkString).mkString

            }

          }

        //-- View
        //-------------------
        case v: View ⇒ li {

          a(v.name, v.fullPath)
        }

        // NO supported, just empty text then
        case _ ⇒ span("Unsupported Node")

      }

    }

    // Build Menu and add it to content
    this.searchByName("menu") match {
      case Some(m) ⇒

        var res = app.navigationConfig.views.map(v ⇒ navElementToNode(v)).toList ::: app.navigationConfig.groups.map(navElementToNode(_)).toList
        m.@->("content", res)

      //println("Adding menu content: " + res)

      case None ⇒
    }

  }

}

trait BootstrapBuilder extends HtmlTreeBuilder {

  // Form
  //----------------
  def bs3Form(cl: ⇒ Any) = {

    form {
      attribute("role" -> "form")
      cl
    }
  }

  def bs3FormGroup(cl: ⇒ Any) = {

    div {
      classes("form-group")
      cl
    }
  }

  // Grid
  //----------------

  def bs3Row(cl: ⇒ Any) = {

    div {
      classes("row")
      cl
    }

  }

  def bs3Col1(cl: ⇒ Any) = {

    div {
      classes("col-md-1")
      cl
    }

  }
  def bs3Col4(cl: ⇒ Any) = {

    div {
      classes("col-md-4")
      cl
    }

  }
  def bs3Col6(cl: ⇒ Any) = {
    div {
      classes("col-md-6")
      cl
    }
  }

  def bs3Col8(cl: ⇒ Any) = {
    div {
      classes("col-md-8")
      cl
    }
  }

  def bs3Col9(cl: ⇒ Any) = {
    div {
      classes("col-md-9")
      cl
    }
  }

  def bs3Col12(cl: ⇒ Any) = {
    div {
      classes("col-md-12")
      cl
    }
  }

  // Tables
  //-----------
  def bs3Table[OT](cl: Table[OT] => Any): Table[OT] = {

    var r = table[OT].asInstanceOf[Table[OT]]
    r("class" -> "table")
    
    cl(r)
    r
  }

}

object Bootstrap3 extends HtmlTreeBuilder {

  def stylesheets(nd: SGGroup[Any]): Unit = {

    nd <= meta {
      attribute(" http-equiv" -> "X-UA-Compatible")
      attribute("content" -> "IE=edge")
    }
    nd <= meta {

      attribute("name" -> "viewport")
      attribute("content" -> "width=device-width, initial-scale=1.0")
    }
    nd <= stylesheet("http://netdna.bootstrapcdn.com/bootstrap/3.0.0/css/bootstrap.min.css")
    nd <= stylesheet("http://netdna.bootstrapcdn.com/bootstrap/3.0.0/css/bootstrap-theme.min.css")

  }

  def scripts(nd: SGGroup[Any]): Unit = {

    nd <= script {
      attribute("src" -> "//netdna.bootstrapcdn.com/bootstrap/3.0.0/js/bootstrap.min.js")
    }

  }

  /**
   * Inject points
   */
  def topNavBar: TopNavbar = {

    // Top navbar
    var top = new TopNavbar
    this.switchToNode(top, {
      classes("navbar navbar-inverse navbar-fixed-top")

      // Container
      div {
        classes("container")

        // Navbar Header
        //---------------------
        div {
          classes("navbar-header")

          currentNode.setName("header")
          currentNode.waitFor("content")

        }

        // Menus
        //-------------------
        div {
          classes("collapse navbar-collapse")
          ul {
            classes("nav navbar-nav")
            currentNode.setName("menu")
            currentNode.waitFor("content")
          }

        }

        // Right part
        //------------------------
        ul {
          classes("nav navbar-nav navbar-right")
        }
      }

    })

    top

  }

}