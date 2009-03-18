<div id="footer">

<?cs if:reference||guide ?>
  <div id="copyright">
    <?cs call:custom_copyright() ?>
  </div>
  <div id="build_info">
    <?cs call:custom_buildinfo() ?>
  </div>
<?cs elif:!hide_license_footer ?>
  <div id="copyright">
    <?cs call:custom_cc_copyright() ?>
  </div>
<?cs /if ?>
  <div id="footerlinks">
    <?cs call:custom_footerlinks() ?>
  </div>

</div> <!-- end footer -->
