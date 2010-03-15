/*! \file FPGA_Device.h
 *  \author Jason Michael Hogan
 *  \brief Include-file for the class FPGA_Device
 *  \section license License
 *
 *  Copyright (C) 2008 Jason Hogan <hogan@stanford.edu>\n
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

#ifndef FPGA_DEVICE_H
#define FPGA_DEVICE_H

#include <STI_Device.h>
#include <FPGA_RAM_Block.h>
#include <EtraxBus.h>

class FPGA_Device : public STI_Device
{
public:
	FPGA_Device(ORBManager* orb_manager, std::string DeviceName, std::string configFilename);

	FPGA_Device(ORBManager* orb_manager, std::string DeviceName, 
			std::string IPAddress, unsigned short ModuleNumber);
	virtual ~FPGA_Device();

private:

	// Device main()
	virtual bool deviceMain(int argc, char* argv[]) = 0;	//called in a loop while it returns true

	// Device Attributes
	virtual void defineAttributes() = 0;
	virtual void refreshAttributes() = 0;
	virtual bool updateAttribute(std::string key, std::string value) = 0;

	// Device Channels
	virtual void defineChannels() = 0;

	// Device Command line interface setup
	virtual void definePartnerDevices() = 0;
	virtual std::string execute(int argc, char** argv) = 0;

	// Device-specific event parsing
	virtual void parseDeviceEventsFPGA(const RawEventMap& eventsIn, SynchronousEventVector& eventsOut) 
		throw(std::exception) = 0;
	virtual double getMinimumEventStartTime() = 0;

	// Event Playback control
	virtual void stopEventPlayback()  = 0;	//for devices that require non-generic stop commands
	virtual void pauseEventPlayback() = 0;	//for devices that require non-generic pause commands


private:
	void FPGA_init();

	void loadDeviceEvents();

	bool playSingleEventFPGA(const RawEvent& rawEvent);
	bool playSingleEventDefault(const RawEvent& event);

	bool readChannel(unsigned short channel, const MixedValue& valueIn, MixedData& dataOut);
	bool writeChannel(unsigned short channel, const MixedValue& value);

	void parseDeviceEvents(const RawEventMap& eventsIn, SynchronousEventVector& eventsOut) 
		throw(std::exception);

private:

	uInt32 RAM_Parameters_Base_Address;
	uInt32 startRegisterOffset;
	uInt32 endRegisterOffset;
	uInt32 eventNumberRegisterOffset;

	uInt32 numberOfEvents;
	bool autoRAM_Allocation;

public:
	FPGA_RAM_Block ramBlock;
	EtraxBus* registerBus;
	EtraxBus* ramBus;
	
	uInt32 getCurrentEventNumber() const;
	virtual short wordsPerEvent() const;
	void waitForEvent(unsigned eventNumber);

private:
	std::string RamStartAttribute;
	std::string RamEndAttribute;
	std::string AutoRamAttribute;

	void autoAllocateRAM();
	bool getAddressesFromController();
	void sendAddressesToController();

	void writeRAM_Parameters();
	uInt32 getMinimumWriteTime(uInt32 bufferSize);

	class FPGA_AttributeUpdater : public AttributeUpdater
	{
	public:
		FPGA_AttributeUpdater(FPGA_Device* device) : 
		  AttributeUpdater(device), device_(device) {}
		void defineAttributes();
		bool updateAttributes(std::string key, std::string value);
		void refreshAttributes();
	private:
		FPGA_Device* device_;
	};

protected:

	template<int N=32>
	class FPGA_BitLineEvent : public BitLineEvent<N>
	{
	public:
		FPGA_BitLineEvent(double time, FPGA_Device* device) : BitLineEvent<N>(time, device), device_f(device) { }
		FPGA_BitLineEvent(const FPGA_BitLineEvent &copy) : BitLineEvent<N>(copy) { }

		//Read the contents of the time register for this event from the FPGA
		uInt32 readBackTime()
		{
			return device_f->ramBus->readDataFromAddress( timeAddress );
		}
		//Read the contents of the value register for this event from the FPGA
		uInt32 readBackValue()
		{
			return device_f->ramBus->readDataFromAddress( valueAddress );
		}

		virtual void waitBeforePlay()
		{
			device_f->waitForEvent( getEventNumber() );
//			cerr << "waitBeforePlay() is finished " << getEventNumber() << endl;
		}

	private:
		virtual void setupEvent()
		{
			time32 = static_cast<uInt32>( getTime() / 10 );	//in clock cycles! (1 cycle = 10 ns)
			timeAddress  = device_f->ramBlock.getWrappedAddress( 2*getEventNumber() );
			valueAddress = device_f->ramBlock.getWrappedAddress( 2*getEventNumber() + 1 );
		}
		virtual void loadEvent()
		{
			//write the event to RAM
			device_f->ramBus->writeDataToAddress( time32, timeAddress );
			device_f->ramBus->writeDataToAddress( getValue(), valueAddress );
		}
		virtual void playEvent(){
			cerr << "playEvent() " << getEventNumber() << endl;
		}
		virtual void collectMeasurementData() = 0;

	private:
		uInt32 timeAddress;
		uInt32 valueAddress;
		uInt32 time32;

		FPGA_Device* device_f;
	};


	typedef FPGA_BitLineEvent<> FPGA_Event;	//shortcut for a 32 bit FPGA event

};

#endif
