# Search Engine
Google? Pff, who uses that anyway!  
This search engine is all you need to find answers to all your questions.

## Introduction
Written in purely functional Scala this seargine has *minimal* side effects!  

This combined with the cutting-edge technologies our team uses makes **Search Engine** the natural choice when uour curiosity has piqued.  

The High Velocity Web Framework **Play** and a **Slick** Functional Relational Mapping enable us to deliver you the fastest and most reliable experience.  
And the backbone of the whole operation is our beloved friend **Spidey**.

## Architecture
Let's get technical now!

### Crawler
> Add a diagram here

We have setup **Spidey** to start crawling from a random Wikipedia page every 10 minutes.  

The crawled pages are then sent to the **LinkDataProcessor** *Monoid* - There key data is extracted for each webpage.
- **Keyword Frequency** - We extract how frequently each word is used on each page.
- **External References** - We extract how many times each page is referenced by others.
- ... And many more in future releases

After all data has been aggregated, it is stored in a MySQL database with the power of Slick.

### Webapp
The web server consists of the following endpoints:
- `/` - Always returns the homepage
- `/search?keyword` - Returns all relevant links containing the given keyword
- ... And many more to come in future releases

The results of the search will be ordered by relevance - Pages where the keyword is used most frequently will be displayed with higher priority.  
In a future release we will be able to further prioritize results by the amount of times the page is referenced by others.  
In another future release we will be able to show more data than just the link url - The title of the page and some related content.

## Usage
### Running
After checking out this repository you will need to run the following command
```bash
sbt run
```
You will then be able to open `localhost:9000` in your browser to see the homepage.
> Insert Screenshot here.

### Tests
Yes, we DO have a couple of tests! To run them execute the following command
```
sbt test
```
*They are run on each push by our continuous integration tool.*
