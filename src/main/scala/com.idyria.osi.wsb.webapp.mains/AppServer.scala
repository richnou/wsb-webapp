/*
 * #%L
 * WSB Webapp
 * %%
 * Copyright (C) 2013 - 2014 OSI / Computer Architecture Group @ Uni. Heidelberg
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package com.idyria.osi.wsb.webapp.mains

import com.idyria.osi.wsb.core.WSBEngine

import com.idyria.osi.wsb.webapp.http.connector._

import com.idyria.osi.wsb.webapp._

/**
 *
 * The AppServer class can start applications and is used as embedded application
 */
class AppServer {

  // Create WSB Engine
  //------------------------
  var engine = new WSBEngine()

  def start() = {

    engine.lInit
    engine.lStart

  }

  def stop = {
    engine.lStop
  }

  // Connectors
  //------------------------

  /**
   * Adds a new HTTP connector to the Engine
   *
   */
  def addHTTPConnector(host: String, port: Int) = {

    var connector = HTTPConnector(port)
    engine.network.addConnector(connector)
    connector

  }

  // Application
  //----------------

  /**
   * Add an application as Broker tree candidate
   *
   */
  def addApplication[T <: WebApplication](app: T): T = {

    engine.broker <= app
    app

  }

}

object AppServer extends App {

  println("Welcome to WSB Webapp App Server")

  println("This Web Server tries to start webapplications2")
  
   // Gather some parameters
   //----------------------
   var port = args
  
  // Create App server
  //------------------------
  var server = new AppServer
  server.addHTTPConnector("localhost", 8080)
    
  
}