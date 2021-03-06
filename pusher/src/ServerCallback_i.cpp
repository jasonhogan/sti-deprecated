/*! \file ServerCallback_i.cpp
 *  \author Jason Michael Hogan
 *  \brief Source-file for the class ServerCallback_i
 *  \section license License
 *
 *  Copyright (C) 2010 Jason Hogan <hogan@stanford.edu>\n
 *  This file is part of the Stanford Timing Interface (STI).
 *
 *  The STI is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  The STI is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with the STI.  If not, see <http://www.gnu.org/licenses/>.
 */

#include "ServerCallback_i.h"

ServerCallback_i::ServerCallback_i()
{
	pinged = false;
	disconnected = false;
}

ServerCallback_i::~ServerCallback_i()
{
}

void ServerCallback_i::pingServer() 
{
	pinged = true;
}

void ServerCallback_i::disconnectFromServer()
{
	disconnected = true;
}

void ServerCallback_i::reset()
{
	pinged = false;
}

bool ServerCallback_i::pingReceived()
{
	return pinged;
}
bool ServerCallback_i::isDisconnected()
{
	return disconnected;
}


