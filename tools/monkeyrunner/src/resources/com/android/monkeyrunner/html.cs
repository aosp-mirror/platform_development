<html>
<body>
<h1>MonkeyRunner Help<h1>
<h2>Table of Contents</h2>
<ul>
<?cs each:item = help ?>
<li><a href="#<?cs name:item ?>"><?cs var:item.name ?></a></li>
<?cs /each ?>
</ul>
<?cs each:item = help ?>
<h2><a name="<?cs name:item ?>"><?cs var:item.name ?></a></h2>
  <p><?cs var:item.doc ?></p>
    <?cs if:subcount(item.argument) ?>
<h3>Args</h3>
<ul>
      <?cs each:arg = item.argument ?>
        <li><?cs var:arg.name ?> - <?cs var:arg.doc ?></li>
      <?cs /each ?>
</ul>
<?cs /if ?>
<?cs /each ?>
</body>
</html>
