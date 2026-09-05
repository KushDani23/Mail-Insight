import { useState } from 'react';
import EmailTable from '../components/EmailTable';
import styles from './EmailsPage.module.css';

export default function EmailsPage({ refreshKey }) {
  return (
    <div className={styles.wrapper}>
      <EmailTable refreshKey={refreshKey} />
    </div>
  );
}
