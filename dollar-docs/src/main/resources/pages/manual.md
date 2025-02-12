#!/usr/bin/env dollar

## Introduction

### What is Dollar?

Dollar is a scripting language for the JVM, it is intended to allow the rapid development of prototype applications or occasional use scripts; especially ones dealing with systems integration. Much in the same way that you would write BASH scripts for system programming.

In a Unix shell, like BASH, we largely script the execution of small programs and operate on files. In Dollar we largely script builtin functions and the transfer of data between URIs.

You should find Dollar familiar if you have worked with BASH and Java as it borrows idioms from both. The exception is the use of reactive programming which may be a little novel to the reader.

### When should I use it?

Dollar is intended to allow the rapid development of prototype applications, small hacks and occasional use scripts; especially ones dealing with systems integration.

Although Dollar is intended to be a complete programming language it is not suited to large scale application development. For that, the author recommends sticking to Java or a similar strongly typed general purpose languages.

### Executable Documentation

Everything in this documentation is executed as part of the build process, so all the examples are guaranteed to run with the latest master branch of Dollar.

Yep Dollar can actually run Markdown files, in fact the source file that this page was built from starts with:

```
#!/usr/bin/env dollar
```
So it can be executed directly from a Unix command line.

The source for this page (minus that header) is [here](manual.md)

### Getting Started

NOTE: At present only Mac OS X and 64 Bit Ubuntu Linux is supported, however since Dollar is entirely based in Java it's trivial to port to other systems. Please add an issue on the GitHub project specifying the platform you'd like to help support.

First download the Dollar scripting runtime from [distribution](http://dollarscript.s3-website-eu-west-1.amazonaws.com/dist/dollar-{{site.release}}.tgz)

Make sure `dollar/bin` is on your PATH.

Run `dollar <filename>` to execute a Dollar script.

Here is an example of what Dollar looks like

```dollar

def testParams {$2 + " " + $1}

@@ testParams ("Hello", "World") 

.: testParams ("Hello", "World") == "World Hello"

```

## Understanding the Basics


Dollar has it's own peculiarities, mostly these exists to help with it's major target: JVM based integration projects. So it's important to understand the basic concepts before getting started.

### Coding Conventions

I've put these at the beginning knowing that they won't make sense until you've read the whole document, so skip this if you need; but having them at the beginning will be handy if you refer back to this document later.

*  **Classes and Types** in CamelCase
*  **variables, functions and fields**, in lowerCamelCase
*  **CONSTANT_VALUES** should be in UPPERCASE
*  **BUILTIN** functions are in UPPERCASE
*  **keywords** are in lower case

Use **`def`** for functions rather than **`const`**. Unless otherwise required define a function using a **block** not a list or map.

When choosing to use a keyword or symbol for an operator that supports both e.g. `print`/`@@` choose the keyword if the visual complexity is too high and choose the symbol if brevity adds clarity. If you're unsure err on the side of keywords.

The following however should usually be used in their operator forms for most scripts as they are widely used and therefore easy for a new developer to pick up: `@@`, `.:`, `<->`, `<=>`, `#`, `<<`, `>>`, `:-`.

An example of when the keyword forms are more useful is the writing of test scripts, in which case `print`, `assert`, `is`, `always`  etc. are clearer than  `@@`, `.:`, `<->`, `<=>` to the reader.

### Reactive Programming

Dollar expressions are by default *lazy*, this is really important to understand otherwise you may get some surprises. This lazy evaluation is combined with a simple event system to make Dollar a [reactive programming language](http://en.wikipedia.org/wiki/Reactive_programming) by default. 

The simplest way to understand reactive programming is to imagine you are using a spreadsheet. When you say a cell has the value SUM(A1:A4) that value will *react* to changes in any of the cells from A1 to A4. Dollar works the same way by default, however you can also *fix* values when you want to write procedural code.

Let's see some of that behaviour in action:

```dollar

var variableA = 1
const variableB := variableA
variableA = 2

.: variableB == 2
```

In the above example we are assigning the variableA to the value 1, we then declare (using the declarative operator `:=`) that variableB is the *same as* variableA. So when we change variableA to 2 we also change variableB to 2.

Before we go any further let's clarify `:=` vs `=`, I have chosen to follow the logic [described here](https://math.stackexchange.com/questions/1838678/confused-about-notation-versus-plain-old) so that the `:=` operator is a definition (and by it's nature reactive) and `=` is an assignment ( not reactive and has an infinite fix depth, more on that later).

This means that `a := b + 1` translates to **a is defined as b + 1** so a is behaving reactively, changes to b cause a change in the value of a. It also means that `a = b + 1` simply assigns `b + 1` to the variable a, changes to b do not cause changes to a. 

At this point it's time to introduce a what is arguably a cleaner and easier to understand short hand for 'const reactiveVar := {...}` the short hand is 'def reactiveVar {...}' such as:

```dollar
    def myFunction { @@ "Hello World"}
```

The `def` keyword implies `const` and it also does not allow dynamic variable names (more on that later). A rule of thumb is if you'd like to have something act like a function use `def`.


**TL;DR `=` behaves like it's Java equivalent, `:=` doesn't and use `def` to create functions.**

> The assertion operator `.:` will throw an assertion error if the value following is either non boolean or not true.


Now let's throw in the causes operator :

```dollar

var a=1
a causes { @@ $1 }
a=2
a=3
a=4

```

~~~
2
3
4
~~~

That simple piece of code will simply output each change made to the variable a, but wait a minute what about ...

```dollar

var b=1
var a=1
a + b + 1 causes { @@ "a=" + a + ", b=" + b}
a=2
a=3
a=4
b=2
```

~~~
a=2, b=1
a=3, b=1
a=4, b=1
a=4, b=2
~~~

Yep, you can write reactive expressions based on collections or arbitrary expressions. When any component changes the right hand side is re-evaluated (the actual value that changed is passed in as $1).

But it's even simpler than that, many of Dollars operators are reactive themselves. That means they understand changes to their values. Take `@@` (or `print`) as an example:

```dollar
var b=1
@@b
b=2
```

Outputs 1 then 2 because @@ heard the change to b and re output the new value. Often this is what you want, however if you don't just add the fix operator `&` before the value. That will stop reactive behaviour.

```dollar
var b=1
@@ &b
b=2
```


### Functional Programming and the 'pure' operator

Support for functional programming is included in Dollar, this will be widened as the language is developed. For now it is provided by the `pure` operator. This signals that an expression or declaration is a pure expression or function.

In this example we're declaring reverse to be an expression that reverses two values from a supplied array. Because we declare it as `pure` the expression supplied must also be `pure`. To understand what a pure function is please see http://en.wikipedia.org/wiki/Pure_function. Basically it prohibits the reading of external state or the setting of external state. We next swap `[2,1]` within a newly created pure expression, which is subsequently assigned to a. If reverse had not been declared pure it would not be allowed within the pure expression.

 ```dollar
 pure def reverse [$1[1],$1[0]]

 a= pure {
     reverse([2,1])
 }

 ```

Note some builtin functions are not themselves pure and will trigger parser errors if you attempt to use them in a pure expression. Take DATE() for example which supplies an external state (the computers clock).


### Assignment and Definition


#### Assignment

```dollar

var variableA = 1
var variableB = variableA
variableA = 2

.: variableB == 1
```

So as you can see when we use the `=` assignment operator we assign the *value* of the right hand side to the variable.

The assignment operator `=` has an infinite 'fix' depth This means that any expression will be evaluated completely also it means the result is not reactive.

The `always` operator `<=>` will compare two values and throw an exception if they are not the same at any point **at or after** the expression,  ` a <=> b`  is the same as `.: a == b`.

The assert equals operator `<->` will compare two values only at the point that the expression occurs. It is roughly the same as .equals() in Java and is the equivalent of `.: &a == &b`

#### Definition

There are two ways of using definitions in Dollar, they are semantically the same but syntactically different. Firstly we can just use the `:=` definition operator. This is not an assignment in the sense that the variable being defined is in fact being assigned the `expression` on the right hand side. Not the value of the expression.

```dollar

const lambdaVar :=  {$1 + 10}
lambdaVar(5) <=> 15

```
In the above example we have parametrized the expression `lambdaVar` with the value `5` and got the value `15`. So we can clearly see that `lambdaVar` is an expression (or lambda) in this case, not a fixed value.

The above looks a lot like a function doesn't it. So to add a little syntactic sugar you can also declare the exact same expression using the `def` syntax below.

```dollar

def lambdaVar  {$1 + 10}
lambdaVar(5) <=> 15

```

Note that `def` implies `const`, `def` means define and therefore not variable.

#### Summary

> It's important to note that all values in Dollar are immutable - that means if you wish to change the value of a variable you *must* __reassign__ a new value to the variable. For example `v++` would return the value of `v+1` it does not increment v. If however you want to assign a constant value, one that is both immutable and cannot be reassigned, just use the `const` modifier at the variable declaration.

```dollar
const MEDIUM = 23
// MEDIUM= 4 would now produce an error
```

So `:=` supports the full reactive behaviour of Dollar, i.e. it is a definition not a value assignment, and `=` is used to nail down a particular value or reduce the reactive behaviour. Later we'll come across the fix operator `&` which instructs Dollar to fix a value completely.

### Blocks

#### Line Block
Dollar supports several block types, the first is the 'line block' a line block lies between `{` and `}` and is separated by either newlines or `;` characters.

```dollar

var myBlock = {
    "Hello "
    "World"
}

myBlock <=> "World"

const myBlock2 = {1;2}

myBlock2 <=> 2

```

When a line block is evaluated the result is the value of the last entry. For advanced users note that all lines will be evaluated, the value is just ignored. A line block behaves a lot like a function in an imperative language.

#### List Block

Next we have the list block, the list block preserves all the values each part is separated by either a `,` or a newline but is delimited by `[` and `]`.

```dollar

var list = [
    "Hello "
    "World"
]

list <=> ["Hello ","World"]

const list2 = [1,2]

list2 <=> [1,2]

```

#### Map Block

Finally we have the map block, when an map block is evaluated the result is the aggregation  of the parts from top to bottom into a map. The map block starts and finishes with the `{` `}` braces, however each part is separated by a `,`  or *newline* not a `;`. The default behaviour of a map block is virtually useless, it takes each value and makes it's String value the key and the original value is the value paired with that key.

```dollar

var mapBlock = {
    "Hello",
    "World"
}

mapBlock <=> {"Hello":"Hello", "World":"World"}

const mapBlock2 = { 1, 2}

mapBlock2 <=> {"1":1,"2":2}

```

Map blocks are combined with the pair `:` operator to become useful and create maps/JSON like this:


```dollar

var mapBlock = {
    "first":"Hello ",
   "second":"World"
}

@@ mapBlock

mapBlock.second <=> "World"

```

A map block with one entry that is not a pair is assumed to be a *Line Block*.

> The stdout operator `@@` is used to send a value to stdout in it's serialized (JSON) format, so the result of the above would be to output `{"first":"Hello ","second":"World"}` a JSON object created using JSON like syntax. Maps can also be created by joining pairs.

```dollar

var pair1 = "first" : "Hello ";
var pair2 = "second" : "World";

.: pair1 + pair2 == {"first":"Hello ","second":"World"}

```

Dollar maps are also associative arrays (like JavaScript) allowing you to request members from them using the list subscript syntax

```dollar
{"key1":1,"key2":2} ["key"+1] <=> 1
{"key1":1,"key2":2} [1] <=> {"key2":2}
{"key1":1,"key2":2} [1]["key2"] <=> 2
```

As you can see from the example you can request a key/value pair (or Tuple if you like) by it's position using a numeric subscript. Or you can treat it as an associative array and request an entry by specifying the key name. Any expression can be used as a subscript, numerical values will be used as indexes, otherwise the string value will be used as a key.


### Lists

Dollar's lists are pretty similar to JavaScript arrays. They are defined using the `[1,2,3]` style syntax and accessed using the `x[y]` subscript syntax.

```dollar
.: [1,2,3] + 4 == [1,2,3,4];
.: [1,2,3,4] - 4 == [1,2,3];
.: [] + 1 == [1] ;
.: [1] + [1] == [1,1];
.: [1] + 1 == [1,1];

[1,2,3][1] <=> 2

```

> Note we're using the assert equals or `<=>` operator here, this is a combination of `.:` and `==` that will cause an error if the two values are not equal.

You can count the size of the list using the size operator `#`.

```dollar
#[1,2,3,4] <=> 4
```


### Ranges

Dollar (at present) supports numerical and character ranges using Maven style syntax


In pseudo-code:
```
(a..b) = {x | a < x < b}
[a..b] = {x | a <= x <= b}
[a..b) = {x | a <= x < b}
(a..b] = {x | a < x <= b}
(a..) = {x | x > a}
[a..) = {x | x >= a}
(..b) = {x | x < b}
(..b] = {x | x <= b}
(..) = all values
```

Please see [the Guava docs](https://github.com/google/guava/wiki/RangesExplained) for more information on the range format used.


```dollar

#("a".."c") <=> 1
#["a".."c"] <=> 3
[1..3][1] <=>2

```


### Scopes & Closure

Dollar makes extensive use of code blocks with scope [closure](https://en.wikipedia.org/wiki/Closure_(computer_programming)). Blocks, lists and maps all have scope closure - I suggest reading [this article by Martin Fowler](https://martinfowler.com/bliki/Lambda.html) if you're unfamiliar with closures. Let's start with a simple example:

```dollar
var outer=10;
def func {
    outer;
}
func() <=> 10;
```

In the above example `func` is a block collection which returns `outer`. It has access to `outer` because at the time of declaration outer is in it's parent's lexical scope.

```dollar

def func {
    var inner=10;
    {$1+inner}
}

func()(10) <=> 20;
```

In the above example we now return an anonymous block collection from func which we then parametrize with the value `10`. When `func` is executed it returns the parametrized block, which we then call with `10` and which adds the value `inner` to the parameter (`$1`) - naturally the result is 20.

So all of that looks fairly familiar if you've ever used JavaScript, but remember all of Dollar's collections have scope closure so the following is valid:

```dollar
var outer=10;

const scopedArray := [$1,outer,{var inner=20;inner}]

scopedArray(5)[0] <=> 5;
scopedArray(5)[1] <=> 10;
scopedArray(5)[2]() <=> 20;

```

In this example the list has lexical scope closure and when we parametrize it using `(5)` we can pass in the positional parameter `($1)` for when it is evaluated.

#### Understanding Scopes A Little Deeper

Each parse time scope boundary (_blocks, lists, maps, constraints, parameters etc._) is marked as such during the initial parse of the script. When executed each of these will create a runtime scope. Each runtime boundary will create a hierarchy of scopes with the previous being the parent.


Where an executable element with scope closure (such as _lists, blocks and maps_) is executed  **all** current scopes are saved and attached to that element. So when the element is subsequently executed it retains it's original lexical closure (as described [here](https://en.wikipedia.org/wiki/Closure_(computer_programming)#Implementation_and_theory)).

>Please look at the `SourceNodeOptions` class for the three types of scoped nodes, they are `NO_SCOPE` which has no effect on the current scope, `NEW_SCOPE` which creates a new scope but does not have closure and `SCOPE_WITH_CLOSURE` which creates a new scope with lexical closure.


### Error Handling

Error handling couldn't be simpler. Define an error expression using the error keyword, the expression supplied will be evaluated on an error occurring within any sub scope of the scope in which it is defined. The special variables `msg` and `type` will be assigned values.

```dollar
var errorHappened= false
error { @@ msg; errorHappened= true }
var a= << http://fake.com:99999
.: errorHappened
```

### Logging

Logging is done by the `print`,`debug` and `err` keywords and the `@@`,`!!` and `!?` operators.

| Keyword  | Operator |
| -------- | -------- |
| `print`  | `@@`     |
| `debug`  | `!!`     |
| `err`    | `!?`     |


```dollar
@@ "I'm a stdout message"
!! "I'm a debug message"
!? "I'm an error message"
```

## Type System
### Intro
Although Dollar has a very loose type system, it does support basic runtime typing and a type prediction system. At present the inbuilt types includes: String, Integer, Decimal, List, Map, URI, Void, Range, Boolean. The value for a type can be checked using the `type` operator:

```dollar
.: "Hello World" type String
.: ["Hello World"] type List
```

### Date

Dollar supports a decimal date system where each day is 1.0. This means it's possible to add and remove days from a date using simple arithmetic.

```dollar
@@ DATE()
@@ DATE() + 1
@@ DATE() - 1

.: DATE() + "1.0" type String
.: DATE() / "1.0" type Decimal
```

Components of the date can be accessed using the subscript operators:

```dollar
@@ DATE().DAY_OF_WEEK

@@ DATE()['DAY_OF_YEAR']=1
```

Valid values are those from `java.time.temporal.ChronoField`

```
NANO_OF_SECOND, NANO_OF_DAY, MICRO_OF_SECOND, MICRO_OF_DAY, MILLI_OF_SECOND, MILLI_OF_DAY, SECOND_OF_MINUTE, SECOND_OF_DAY, MINUTE_OF_HOUR, MINUTE_OF_DAY, HOUR_OF_AMPM, CLOCK_HOUR_OF_AMPM, HOUR_OF_DAY, CLOCK_HOUR_OF_DAY, AMPM_OF_DAY, DAY_OF_WEEK, ALIGNED_DAY_OF_WEEK_IN_MONTH, ALIGNED_DAY_OF_WEEK_IN_YEAR, DAY_OF_MONTH, DAY_OF_YEAR, EPOCH_DAY, ALIGNED_WEEK_OF_MONTH, ALIGNED_WEEK_OF_YEAR, MONTH_OF_YEAR, PROLEPTIC_MONTH, YEAR_OF_ERA, YEAR, ERA, INSTANT_SECONDS, OFFSET_SECONDS
```

As you can see we can do date arithmetic, but thanks to another Dollar feature anything that can be specified as xxx(i) can also be written i xxx (where i is an integer or decimal and xxx is an identifier). So we can add days hours and seconds to the date.

```dollar
@@ DATE() + 1 DAY
@@ DATE() + 1 HOUR
@@ DATE() + 1 SEC
```

Those values are built in, but we can easily define them ourselves.

```dollar
def fortnight ($1 * 14)

@@ DATE() + 1 fortnight
```
### STRING
### INTEGER
### DECIMAL
### LIST
### MAP
### URI
### VOID
### NULL
### RANGE
### BOOLEAN

### Constraints

Although there are limited compile time type constraints (using the predictive type system) in Dollar a runtime type system can be built using constraints. Constraints are declared at the time of variable assignment or declaration. A constraint once declared on a variable cannot be changed. The constraint is placed before the variable name at the time of declaration in parenthesis.

```dollar
var (it < 100) a = 50
var (previous type Void|| it > previous) b = 5
b=6
b=7
var ( it type String) s="String value"
```

The special variables `it` - the current value and `previous` - the previous value, will be available for the constraint.

To build a simple runtime type system simply declare (using `:=`) your type as a boolean expression.

```dollar

//define a pseudo-type
def colorEnum ( it in ["red","green","blue"] )


//Use it as a constraint
var (colorEnum) myColor= "green"

error { @@ msg }

//This fails
var myColor="apple"

```

Of course since the use of `(it type XXXX)` is very common Dollar provides a specific runtime type constraint that can be added in conjunction with other constraints. Simply prefix the assignment or declaration with `<XXXX>` where XXXX is the runtime type.


```dollar
var <String> (#it > 5) s="String value"
```

It is intended that the predictive type system combined with runtime types will help to spot a few more bugs at compile time.

### Type Coercion
Dollar also supports type coercion, this is done using the `as` operator followed by the type to coerce to.


```dollar
var <String> s= 1 as String
s <=> "1"
```

A few more examples follow.

```dollar
1 as String <=> "1"
1 as Boolean <=> true
1 as List <=> [1]
1 as Map <=> {"value":1}
1 as Void <=> void
1 as Integer <=> 1

"1" as Integer <=> 1
"http://google.com" as URI
"1" as Void <=> void
"true" as Boolean <=> true
"1" as Boolean <=> false
"1" as List <=> ["1"]
"1" as Map <=> {"value":"1"}
"1" as String <=> "1"

true as String <=> "true"
true as Integer <=> 1
true as List <=> [true]
true as Map <=> {"value":true}
true as Boolean <=> true
true as Void <=> void


[1,2,3] as String <=> "[ 1, 2, 3 ]"
[1,2,3] as List <=> [1,2,3]
[1,2,3] as Boolean <=> true
[1,2,3] as Map <=> {"value":[1,2,3]}

{"a":1,"b":2} as String <=> '{"a":1,"b":2}'
{"a":1,"b":2} as List <=> ["a":1,"b":2]
{"a":1,"b":2} as Boolean <=> true
{"a":1,"b":2} as Void <=> void
```


## Imperative Control Flow

With imperative control flow, the control flow operations are only triggered when the block they are contained within is evaluated. I.e. they behave like control flow in imperative languages. So start with these if you're just learning Dollar.

### If

Dollar supports the usual imperative control flow but, unlike some languages, everything is an operator. This is the general rule of Dollar, everything has a value. Dollar does not have the concept of statements and expressions, just expressions. This means that you can use control flow in an expression.

```dollar

var a=1
var b= if a==1 2 else 3
b <=> 2

```

So let's start with the `if` operator. The `if` operator is separate from the `else` operator, it simply evaluates the condition supplied as the first argument. If that value is boolean and true it evaluates the second argument and returns it's value; otherwise it returns boolean false.

The `else` operator is a binary operator which evaluates the left-hand-side (usually the result of an `if` statement), if that has a value of false then the right-hand-side is evaluated and it's result returned, otherwise it returns the left-hand-side.

The combined effect of these two operators is to provide the usual if/else/else if/ control flow

```dollar

var a=5
//Parenthesis added for clarity, not required.
var b= if (a == 1) "one" else if (a == 2) "two" else "more than two"
.: b == "more than two"

```

### For

```dollar

for i in [1..10] {
    @@ i
}

```

### While

```dollar
var a= 1
while a < 10 {
 a= a+1
}
a <=> 10

```



## Reactive Control Flow

### Causes

Dollar is a reactive programming language, that means that changes to one part of your program can automatically affect another. Consider this a 'push' model instead of the usual 'pull' model.

Let's start with the simplest reactive control flow operator, the '=>' or 'causes' operator.

```dollar
var a=1; var b=1

a => (b= a)

a <-> 1 ; b <-> 1

a=2 ; a <-> 2 ; b <-> 2

```

Okay so reactive programming can melt your head a little. So let's go through the example step by step.

Firstly we assign fixed values to `a` and `b`, we then say that when `a` changes the action we should take is to assign it's value to `b`. Okay now we check to see if the current value of `a` is equal to 1 (using the imperative assert equals or `is` operator `<->`).

We then do the same with b to see if it is 1.

Next we assign a new value of 2 to `a`. This will immediately (within the same thread) trigger the reactive `=>` operator which is triggered by changes to `a`. The trigger assigns the value of `a` to `b`, so `b` is now the same as `a`. The assertions at the end confirm this.

### When

Next we have the 'when' operator which can be specified as a statement, usually for longer pieces of code. Or as the `?` operator, for concise code.


```dollar

var c=1
var d=1

//When c is greater than 3 assign it's value to d
c > 3 ? (d= c)

c <-> 1; d <-> 1
c= 2; c <-> 2; d <-> 1
c= 5 ; c <-> 5 ; d <-> 5

```

This is similar to the previous example except that we have to set a value greater than 3 for the action to be taken.

```dollar
//Note alternative syntax is when <condition> <expression>
var c=1
when c > 3 { @@ c}
```

### Collect


The `collect` operator listens for changes in the supplied expression adding all the values to a list until the `until` clause is triggered. It then evaluates the second expression with the values `it` for the current value, `count` for the number of messages **received** since last emission and `collected` for the collected values. The whole operator itself emits `void` unless the collection operation is triggered in which case it emits the collection itself. Values can be skipped with an `unless` clause. Skipped messages increase the count value, so use `#collected` if you want the number of collected values.

```dollar

var e=void

//Length is greater than or equal to 4 unless void
var (#it >= 4 || it type Void) collectedValues=void

//count starts at 0 so this means five to collect (except if it contains the value 10)
collect e until count == 4 unless it == 10 {
    print count
    print collected
    collectedValues= collected
}

e=1; e=2; e=3; e=4; e=5; e=6
collectedValues <-> [1,2,3,4,5]
e=7; e=8; e=9; e=10
collectedValues <-> [6,7,8,9]
e=11; e=12; e=13; e=14; e=15; e=16
collectedValues <-> [11,12,13,14,15]

```

## Parameters &amp; Functions

In most programming languages you have the concept of functions and parameters, i.e. you can parametrize blocks of code. In Dollar you can parametrize *anything*. For example, let's just take a simple expression that adds two strings together, in reverse order, and pass in two parameters.

```dollar
($2 + " " + $1)("Hello", "World") <=> "World Hello"

```

The naming of positional parameters is the same as in shell scripts.

Now if we take this further we can use the declaration operator `:=` to say that a variable is equal to the expression we wish to parametrise, like so:

```dollar

const testParams := ($2 + " " + $1)
testParams ("Hello", "World") <=> "World Hello"

```

Yep we built a function just by naming an expression. You can name anything and parametrize it - including maps, lists, blocks and plain old expressions.


What about named parameters, that would be nice.

```dollar
const testParams := (last + " " + first)
testParams(first="Hello", last="World") <=> "World Hello"
```

Yep you can use named parameters, then refer to the values by the names passed in.


## Resources &amp; URIs

URIs are first class citizen's in Dollar. They refer to a an arbitrary resource, that can be accessed using the specified protocol and location. Static URIs can be referred to directly without quotation marks, dynamic URIs can be built by casting to a uri using the `as` operator.

```dollar
var posts = << https://jsonplaceholder.typicode.com/posts 
var titles = posts each { $1.title }
@@ titles
```

In this example we've requested a single value (using `<<`) from a uri and assigned the value to `posts` then we simply iterate over the results  using `each` and each value (passed in to the scope as `$1`) we extract the `title`. The each operator returns a list of the results and that is what is passed to standard out.


## Using Other Languages

Hopefully you'll find Dollar a useful and productive language, but there will be many times when you just want to quickly nip out to a bit of another language. To do so, just surround the code in back-ticks and prefix with the languages name. Currently only `java` is supported but more will be added soon.

```dollar

var variableA="Hello World"

var java = java `out=in.get(0);` (variableA)

java <=> "Hello World"

```
### Java

A whole bunch of imports are done for you automatically (see below) but you will have to fully qualify any thirdparty libs.
 
> imports `dollar.lang.*``dollar.internal.runtime.script.api.*` `com.sillelien.dollar.api.*` `java.io.*` `java.math.*` `java.net.*` `java.nio.file.*` `java.util.*` `java.util.concurrent.*` `java.util.function.*` `java.util.prefs.*` `java.util.regex.*` `java.util.stream.*`
 
 > static imports `DollarStatic.*` `dollar.internal.runtime.script.java.JavaScriptingStaticImports.*`

The return type will be of type `var` and is stored in the variable `out`. The Java snippet also has access to the scope (Scope) object on which you can get and set Dollar variables.

Reactive behaviour is supported on the Scope object with the `listen` and `notify` methods on variables. You'll need to then built your reactivity around those variables or on the `out` object directly (that's a pretty advanced topic).


## Operators

### Iterative Operators

### Comparison Operators

### Numerical Operators

Dollar support the basic numerical operators +,-,/,*,%,++,-- as well as #

**Remember ++ and -- do not change a variable's value they are a shorthand for a+1 and a-1 not a=a+1 or a=a-1**

```dollar

 1 + 1 <=> 2
 3 -2 <=> 1
 2 * 2 <=> 4
 5 / 4 <=> 1
 5 % 4 <=> 1
 5.0 /4 <=> 1.25
 # [1,2,3] <=> 3
 # 10 <=> 1
 10++ <=> 11
 10-- <=> 9

```

And similar to Java, Dollar coerces types as required:

```dollar
.: (1 - 1.0) type Decimal
.: (1.0 - 1.0) type Decimal
.: (1.0 - 1) type Decimal
.: (1 - 1) type Integer

.: (1 + 1.0) type Decimal
.: (1.0 + 1.0) type Decimal
.: (1.0 + 1) type Decimal
.: (1 + 1) type Integer

.: 1 / 1 type Integer
.: 1 / 1.0 type Decimal
.: 2.0 / 1 type Decimal
.: 2.0 / 1.0 type Decimal

.: 1 * 1 type Integer
.: 1 * 1.0 type Decimal
.: 2.0 * 1 type Decimal
.: 2.0 * 1.0 type Decimal


.: 1 % 1 type Integer
.: 1 % 1.0 type Decimal
.: 2.0 % 1 type Decimal
.: 2.0 % 1.0 type Decimal
.: ABS(1) type Integer
.: ABS(1.0) type Decimal
```

### Logical Operators

Dollar supports the basic logical operators &&,||,! as well as the truthy operator `~` and the default operator `:-`.

#### Truthy
The truthy operator `~` converts any value to a boolean by applying the rule that: void is false, 0 is false, "" is false, empty list is false, empty map is false - all else is true.

```dollar

.: ~ [1,2,3]
.: ! ~ []
.: ~ "anything"
.: ! ~ ""
.: ~ 1
.: ! ~ 0
.: ! ~ {void}
.:  ~ {"a" : 1}
.: ! ~ void

void :- "Hello" <=> "Hello"
1 :- "Hello" <=> 1

```
#### Boolean Operators

The shortcut operators `||` and `&&` work the same as in Java. As do the comparison operators `>`,`<` etc. They also have keyword alternatives such as `and` or `or`.

| Keyword              | Operator | Java Equivalent           |
| --------             | -------- | ---------                 |
| `and`                | `&&`     | `&&`                      |
| `or`                 | `ǀǀ`     | `ǀǀ`                      |
| `equal`              | `==`     | `.equals()`               |
|                      | `!=`     | `! .equals()`             |
|                      | `<`      | `lhs.compareTo(rhs) < 0`  |
|                      | `>`      | `lhs.compareTo(rhs) > 0`  |
|                      | `<=`     | `lhs.compareTo(rhs) <= 0` |
|                      | `>=`     | `lhs.compareTo(rhs) >= 0` |


Examples:

```dollar
true && true <=> true
true && false <=> false
false && true <=> false
false && false <=> false

true and true always true

true || true <=> true
true || false <=> true
false || true <=> true
false || false <=> false

false or false always false

.: 1 < 2
.: 3 > 2
.: 1 <= 1
.: 1 <= 2
.: 1 > 0
.: 1 >= 1
.: 2 >= 1
.: 1 == 1
.: "Hello" == "Hello"
.: "abc" < "abd"
```

#### Default Operator

The default operator ':-' (keyword `default`) returns the left hand side if it is not `VOID` otherwise it returns the right hand side.

```dollar
void :- "Hello" <=> "Hello"
1 :- "Hello" <=> 1
void default 2 <=> 2
```


### Pipe Operators
### Remaining Operators
## Imports &amp; Modules
### Import
### Modules

Modules can be imported using the `module` keyword and a string representing in URI format the location of the module. At present the standard format is the Github locator so we're going to look at that first.

```dollar
const chat:= module "github:neilellis:dollar-example-module::chat.ds" (channel="test")
var sub= chat.server()
chat.stop_()
```

Let's start by breaking down the module URI. Okay our first part says it's the GitHub scheme and so the GitHub module locator will be used. Next we say the repository belongs to neilellis and the repository is called dollar-example-module. The next part is the optional branch indicator - here we are using the master, so we just leave that empty. Finally we can optionally supply the name of a single script we want to import. If we don't provide that then the main script from the modules 'module.json' file will be used.

The source for the module can be found here: https://github.com/neilellis/dollar-example-module

You will need to have the `git` command on your path and to have access to the repository using `git clone`.

The GitHub resolver will checkout the specified repository and store it under `~/.dollar/runtime/modules/github/<username>/<repo-name>/<branch>` all further interaction with the module will then be done from the checked out version. If you already have a version checked out a git pull will be done to update the branch.

```dollar
const hello := module "github:neilellis:dollar-example-module:0.1.0:branch.ds"
@@ hello
```

### Module Locators
### Writing Modules

Modules consist of a file called module.json with the name of the main script for the module and an optional array of Maven style java dependencies. And then one or more Dollar files.

//TODO: change module.json to module.ds

```
{
"main":"chat.ds",
"dependencies":["org.twitter4j:twitter4j-core:4.0.2"]
}
```

The Dollar files should use the export modifier on assignments that it wishes to make available to client applications and it can refer to variables that don't exist, in which case values for those variables will need to be passed as parameters to the module declaration in the client application.


```dollar
var redis= ("redis://localhost:6379/" + ${channel :- "test"}) as URI
var www= (("http:get://127.0.0.1:8111/" + ${channel :- "test"}) as URI)

export def server  {
           www subscribe {
            $1.params >> redis
            { body :  all redis }
        }
    };

export def stop_ {STOP(www);STOP(redis); @@ [STATE(www),STATE(redis)]}

export def state_ [STATE(www),STATE(redis)]
```


## Builtin Functions

## Concurrency & Threads

Notes:

All types are immutable, including collections.
You cannot reassign a variable from a different thread unless it is declared as `volatile`.


### Parallel &amp; Serial Lists
The parallel operator `|:|` or `parallel` causes a list to be evaluated in parallel, otherwise it is executed in serial even if the current expression is being evaluated in parallel.

```dollar

const a = [ TIME(), {SLEEP(1 SEC); TIME();}, TIME() ];
const b = |:| [ TIME(), {SLEEP(1 SEC); TIME();}, TIME() ];
//Test different execution orders
.: a[2] >= a[1]
.: b[2] < b[1]
```

### Fork

The fork operator `-<` or `fork` will cause an expression to be evaluated in the background and any reference to the forked expression will block until a value is ready.

```dollar
const sleepTime := {@@ "Background Sleeping";SLEEP(4 SECS); @@ "Background Finished Sleeping";TIME()}
//Any future reference to c will block until c has completed evaluation
var c= fork sleepTime
SLEEP(1 SEC)
@@ "Main thread sleeping ..."
SLEEP(2 SECS)
@@ "Main thread finished sleeping ..."
var d= TIME()
.: c > d
```

In the example the value of c is greater than d because the value of c is evaluated in the background. Note that as soon as you make use of the value of c you block until the value is ready. This is exactly the same as Java's Futures.

## Advanced Topics

TODO

