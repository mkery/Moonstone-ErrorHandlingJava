# Error-Handlers_Eclipse-Plugin
A Spring/Summer 2016 project to build better programmer support for Java exception handling in an IDE.

## Features
![Feature Diagram](/Design/FeatureDiagram.png)
### 1. Auto-comment offending lines
When a try block contains a block of code, it can become unclear which statement is a risky one that may actually throw exception. With what-throws-what hidden away in documentation (the programmer must hover over each statement to read docs and find the offending one in Eclipse), it also is unclear which exception classes to catch. We add auto-comments:
```java
foo(); //may throw FooException
```
These are called "ghost comments" because they may appear in the user's code when the use is looking at a catch block. The use can then choose to add these comments to their code, but we won't add permanent comments without their permission.

![Ghost Comments](/Design/OffendingLinePrototype.png)
### 2. Highlight control flow in try/catch/finally blocks (above image)
There are common issues around not closing resources on BOTH the normal path and error path, or not setting some variable on BOTH the normal path and error path. We can make which code will be skipped in each case more visible through highlighting.

### 3. View where a throw Exception goes and where it is handled
If an error case doesn't make sense to handle locally, the programmer can propagate it up:
```java
throw new SQLException(e,"database write failed in MethodB");
```
However once the exception is thrown, it's not always clear what the next caller is expecting in terms of error condition, or where this exception is eventually handled. Does it get where it needs to, in terms of showing the user something, etc?

We will show a summarize view of the propagation path that allows programmers to answer specific questions about when and how their exception is handled.

### 4. Bad practice detection and fix suggestions
![Bad Practice Fix](/Design/BadPractices_prototype.png)
There are a number of common bad practices people make with exceptions. Some of these are controversial. We can flag possibly bad code with icons, and give quick-fix suggestions. We will also leave the option to ignore or turn off certain warnings, if they don't make sense for the programmer's code.

### 5. Supplementary webpage that the tool can link to, with more information on bad/good practices
Exception handling, in it's details, can be both confusing and heavily debated in terms of what is best. Since only short explanations will fit comfortably in an IDE, these will link to a page we'll create with longer explanation of good/bad practices, supported by links that, for example, tell both sides of an argument.

###6-7. Auto-complete for handlers, Browse past examples of similar handlers
When writing a new exception handler, programmers may be able to write more quickly and more accurately by having some autocomplete for exception handling. Since exception handling is usually quite context-sensitive, we will supplement auto-complete with code examples from their own project's past handlers. This may make implicit policies, such as how to show the user an error message, clear to programmers. 
![Browse Examples](/Design/BrowseExamples_prototype.png)
