# Code Room prototype

<!-- impeccable:product-schema 1 -->

## Platform
web

## Stack
User-requested HTML prototype; plain HTML, CSS, JavaScript and Canvas, without build dependencies. The planned production app remains Java Swing.

## Product Purpose
A playable single-room coding escape game. Read Java code on collected notes, infer its output, enter the matching lock, collect a key and escape.

## Capabilities and Constraints
One initial note, a four-digit drawer lock, a 3×3 wall dial, a second note revealed by the drawer, a key revealed by the wall dial, and an exit door. Bottom inventory selection changes the held clue on the left. User supplied pixel-room and inventory references. Korean UI.

## Open decisions
Prototype title and room decorations are illustrative. Beginner Java array and loop puzzles are an implementation assumption. This prototype does not execute arbitrary Java or call an AI API.

## Confirmed presentation correction
The user rejected a website-style shell. The room must occupy the entire viewport, with only a held clue overlay on the left and game inventory below. No website header, page margins, or dashboard layout. The paper can be folded away; inventory selection reopens it.
