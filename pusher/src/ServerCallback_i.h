/*! \file ServerCallback_i.h
 *  \author Jason Michael Hogan
 *  \brief Include-file for the class ServerCallback_i
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

#ifndef SERVERCALLBACK_I_H
#define SERVERCALLBACK_I_H

#include "pusher.h"

class ServerCallback_i : public POA_STI::Pusher::ServerCallback
{
public:

	ServerCallback_i();
	~ServerCallback_i();

	void reset();
	bool pingReceived();
	bool isDisconnected();

	void pingServer();
	void disconnectFromServer();

private:

	bool pinged;
	bool disconnected;

};

#endif