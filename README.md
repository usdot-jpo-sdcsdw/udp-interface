# UDP Interface

US Department of Transportation Joint Program office (JPO) Situation Data Clearinghouse/Situation Data Warehouse MVP (SDC/SDW MVP)

In the context of ITS, the SDC/SDW is a data warehouse and distribution software system that stores data received from the [JPO-ODE] (https://github.com/usdot-jpo-ode/jpo-ode) system. This data is persisted for consumption by applications & systems supporting the operation, maintenance, and use of the transportation system, as well as related research and development efforts.

Currently the SDC/SDW stores and distributes Traveler Information Messages (TIMs). The UDP Interface of the mvp SDC/SDW allows users to programmatically receive these TIMS bundled as Advisory Situation Data Distributions. These distributions contain up to 4 Advisory Situation Bundles, each composed of up to 10 Advisory Broadcasts. The advisory broadcasts contain the encoded traveler information message (TIM). To reveive data from this interface, a specific UDP dialog sequence must be followed. The below diagram depicts this sequence. 

![UDP Dialog Sequence](images/udp_dialog_sequence.png)

**Each of these messages are UPER encoded.

## Getting Started

TODO

### Prerequisites

TODO

```
Give examples
```

### Installing

A step by step series of examples that tell you have to get a development env running

Say what the step will be

```
Give the example
```

And repeat

```
until finished
```

End with an example of getting some data out of the system or using it for a little demo

## Running the tests

Explain how to run the automated tests for this system

### Break down into end to end tests

Explain what these tests test and why

```
Give an example
```

### And coding style tests

Explain what these tests test and why

```
Give an example
```

## Deployment

Add additional notes about how to deploy this on a live system

## Built With

* [Maven](https://maven.apache.org/) - Dependency Management


## Contributing

TODO

## Versioning

TODO


## License

This project is licensed under the Apache License - see the [LICENSE.md](LICENSE.md) file for details


