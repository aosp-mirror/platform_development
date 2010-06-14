MonkeyRunner help
<?cs each:item = help ?>
<?cs var:item.name ?>
  <?cs var:item.doc ?>

<?cs if:subcount(item.argument) ?>  Args:<?cs each:arg = item.argument ?>
    <?cs var:arg.name ?> - <?cs var:arg.doc ?><?cs /each ?>
<?cs /if ?><?cs /each ?>
