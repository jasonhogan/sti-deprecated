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

#include "STI_Device.h"
#include "FPGA_RAM_Block.h"
#include "EtraxBus.h"

#include "AttributeUpdater.h"
#include "BitLineEvent.h"

#include <boost/thread/locks.hpp>
#include <boost/thread.hpp>

namespace STI
{
namespace Device
{

typedef STI::TimingEngine::TimingEventGroupMap RawEventMap;
using STI::TimingEngine::SynchronousEventVector;
typedef STI::Utils::MixedValue MixedData;
using STI::Utils::MixedValue;

class FPGA_Device : public STI_Device
{
public:
//	FPGA_Device(ORBManager* orb_manager, std::string DeviceName, std::string configFilename);
	FPGA_Device(std::string DeviceName, std::string IPAddress, unsigned short ModuleNumber);

//	FPGA_Device(ORBManager* orb_manager, std::string DeviceName, 
//			std::string IPAddress, unsigned short ModuleNumber);
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
	virtual void parseDeviceEventsFPGA(const STI::TimingEngine::TimingEventGroupMap& eventsIn, STI::TimingEngine::SynchronousEventVector& eventsOut) 
		throw(std::exception) = 0;
	virtual double getMinimumEventStartTime() = 0;

	// Event Playback control
	void stopEventPlayback();	//for devices that require non-generic stop commands
	virtual void pauseEventPlayback() = 0;	//for devices that require non-generic pause commands


private:
	void FPGA_init();

	void loadDeviceEvents();

//	bool playSingleEventFPGA(const RawEvent& rawEvent);
//	bool playSingleEventDefault(const RawEvent& event);

	bool readChannel(unsigned short channel, 
		const STI::Utils::MixedValue& commandIn, STI::Utils::MixedValue& measurementOut);
	bool writeChannel(unsigned short channel, const STI::Utils::MixedValue& commandIn);

	void parseDeviceEvents(const RawEventMap& eventsIn, SynchronousEventVector& eventsOut) 
		throw(std::exception);

private:

	unsigned long pollTime_ms;

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
	
	uInt32 getCurrentEventNumber();
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

	void sleepwait(unsigned long secs, unsigned long nanosecs = 0);

//	omni_mutex* waitForEventMutex;
//	omni_condition* waitForEventTimer;
	mutable boost::shared_mutex waitForEventMutex;
	boost::condition_variable_any waitForEventTimer;

	class FPGA_AttributeUpdater : public STI::Device::AttributeUpdater
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
	class FPGA_BitLineEvent : public STI::TimingEngine::BitLineEvent<N>
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
			//Have the cpu sleep until the event is almost ready.  As a result, the cpu may (theoretically)
			//get slightly behind the FPGA.  Of course, the FPGA will always follow hard timing.  The slight 
			//asynchronicity between the cpu and FPGA is not important, and the benefit is reduced polling 
			//of the event counter register.

			sleepUntil( getTime() );

			//Now check the event counter until this event actually plays.
			device_f->waitForEvent( getEventNumber() );
//			cerr << "waitBeforePlay() is finished " << getEventNumber() << endl;
		}

		virtual void sleepUntil(uInt64 time)
		{
			unsigned long wait_s;
			unsigned long wait_ns;

			statusMutex->lock();
			{
				Int64 wait = static_cast<Int64>(time) - device_f->getCurrentTime() ;

//cout << "FPGA_Device::sleepUntil::wait = " << wait << endl;
				if(wait > 0 && !played)
				{
					//calculate absolute time to wake up
					omni_thread::get_time(&wait_s, &wait_ns, 
						Clock::get_s(wait), Clock::get_ns(wait));

					playCondition->timedwait(wait_s, wait_ns);
				}
			}
			statusMutex->unlock();
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
//			cerr << "playEvent() " << getEventNumber() << endl;
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

}
}

#endif
