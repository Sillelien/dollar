#THIS IS ALL OUT OF DATE, please check out http://neilellis.github.io/dollar for details on DollarScript, I will get round to updating this eventually :-)
[![Gitter](https://badges.gitter.im/Join Chat.svg)](https://gitter.im/neilellis/dollar?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)



dollar
======

[![Circle CI](https://circleci.com/gh/neilellis/dollar.png?style=badge)](https://circleci.com/gh/neilellis/dollar)

Making Java dynamic and easy to use.

Install using Maven

        <dependency>
            <groupId>me.neilellis</groupId>
            <artifactId>dollar-core</artifactId>
            <version>0.0.X</version>
        </dependency>

We're not on Maven central yet but we are on Bintray:

[ ![Download](https://api.bintray.com/packages/neilellis/maven/dollar/images/download.png) ](https://bintray.com/neilellis/maven/dollar/_latestVersion)


NOTES
=====

This is not even alpha software and is under rapid development. If you'd like to share ideas or contribute than this is a good time - however, do not write software that depends on this yet.

Dollar currently has a direct dependency on a Redis server running for it's distributed functions. This will all become optional in later releases. Right now just run one on localhost or set

    -Ddollar.redis=<hostname>

FAQ
===

Why Dollar ?
------------

Once you've written some Dollar code you'll get the reason pretty quickly.

Is this like jQuery for Java?
-----------------------------

No, but Dollar uses a lot of ideas that jQuery popularized, so I would certainly acknowledge our debt to jQuery.

Then what is it?
----------------

If you like the ease of JavaScript, Ruby, Groovy etc. but also enjoy being able to work within the Java language then this is for you. You can write typesafe code and then drop into typeless Dollar code whenever you need to. Dollar is both an alternative paradigm and a complementary resource.


Examples
========

Basic
-----

You can just use dollar to write dynamic JSON oriented (JSON is not a requirement, you can work with maps too) using a fluent format like this:

        int age = new Date().getYear() + 1900 - 1970;
        var profile = $("name", "Neil")
                .$("age", age)
                .$("gender", "male")
                .$("projects", $array("snapito", "dollar"))
                .$("location",
                        $("city", "brighton")
                                .$("postcode", "bn1 6jj")
                                .$("number", 343)
                );

or using a more builder format like this:

        var profile = $(
                $("name", "Neil"),
                $("age", new Date().getYear() + 1900 - 1970),
                $("gender", "male"),
                $("projects", $jsonArray("snapito", "dollar")),
                $("location",
                        $("city", "brighton"),
                        $("postcode", "bn1 6jj"),
                        $("number", 343)
                ));

and these hold true:

        assertEquals(age /11, (int)profile.$("$['age']/11").$int());
        assertEquals("male", profile.$("$.gender").$());
        assertEquals(10, profile.$("5*2").$());
        assertEquals(10, (int)$eval("10").$int());
        assertEquals($("{\"name\":\"Dave\"}").$("name").$$(),"Dave");
        assertEquals($().$("({name:'Dave'})").$("name").$$(), "Dave");

Script
------


To write Dollar 'scripts' you just need to extend the Script class, add a static initializer for $THIS and then put your code within braces.

    public final class FirstScript extends Script {
        static {
            $THIS = FirstScript.class;
        }

        {
            var profile = $("name", "Neil")
                    .$("age", new Date().getYear() + 1900 - 1970)
                    .$("gender", "male")
                    .$("projects", $jsonArray("snapito", "dollar"))
                    .$("location",
                            $("city", "brighton")
                                    .$("postcode", "bn1 6jj")
                                    .$("number", 343)
                    );
            profile.pipe(ExtractName.class).pipe(WelcomeMessage.class).out();
        }
    }

You have access to in and out variables for pipelining, as shown below:

    public class ExtractName extends Script {
        static {
            $THIS = ExtractName.class;
        }

        {
            out = in.$("name");
        }
    }


Webserver
---------

Create a webserver at http://localhost:4567/headers and return the request headers as a JSON string.

        GET("/headers", (context) -> context.headers());

Characteristics
===============

Dollar is designed for production, it is designed for code you are going to have to fix. Every library and language has it's sweet spot. Dollar's sweetspot is working with schema-less data in a production environment. It is not designed for high performance systems (there is a 99.9% chance your project isn't a high performance system) but there is no reason to expect it to be slow either. Where possible the code has been written with JVM optimization in mind.

With this in mind the following are Dollar's characteristics:

* Simple - Dollar does do not expose unnecessary complexity to the programmer, we keep it hidden.

    > The secret of success is to be like a duck – smooth and unruffled on top, but paddling furiously underneath.”

* Typeless - if you *need* strongly typed code stop reading now. If you're writing internet centric modest sized software this is unlikely to be the case.
* Synchronous - asynchronous flows are hard to follow and even harder to debug in production. We do not expose asynchronous behaviour to the programmer.
* Metered - key execution's are metered using Coda Hale's metrics library, this makes production monitoring and debugging easier.
* Nullsafe - Special null type reduces null pointer exceptions, which can be replaced by an isNull() check.
* Threadsafe - No shared state, always copy on write. No shared state means avoidance of synchronization primitives, reduces memory leaks and generally leaves you feeling happier. It comes at a cost (object creation) but that cost is an acceptable cost as far as Dollar is concerned.

The Rules
=========

1. Do not create your own Threads.
2. Do not create your own Threads.
3. Always run from a *static* context (e.g. a public static void main method)
4. All `var` objects are **immutable**, so use the returned value after 'mutation' actions.






