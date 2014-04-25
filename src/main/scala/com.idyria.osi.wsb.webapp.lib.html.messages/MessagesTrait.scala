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
package com.idyria.osi.wsb.webapp.lib.html.messages

import com.idyria.osi.wsb.webapp.view.WebappHTMLBuilder

/**
 * Trait that offers utility functions to handle application messages
 */
trait MessagesBuilder extends WebappHTMLBuilder {

  /**
   * Handle Errors
   */
  def errors = {

    div {
      id("errors")

      /**
       * Consume Errors
       */
      request.consumeErrors {
        error =>

          //-- Produce message div for current error
          div {
            classes("alert alert-danger")
            text(error.getMessage())
          }
      }
    }

  }

}